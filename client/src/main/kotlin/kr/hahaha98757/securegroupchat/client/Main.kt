package kr.hahaha98757.securegroupchat.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kr.hahaha98757.securegroupchat.common.PORT
import kr.hahaha98757.securegroupchat.common.exit
import kr.hahaha98757.securegroupchat.common.isNotValid
import kr.hahaha98757.securegroupchat.common.printErr
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.security.KeyPairGenerator
import kotlin.time.Duration.Companion.milliseconds

fun main(): Unit = runBlocking {
    println("Copyright (c) 2026 hahaha98757 (MIT License)")
    println("Secure Group Chat (client) v1.0.0")
    println("공식 사이트: https://github.com/hahaha98757/secure-group-chat")
    println()
    delay(1000.milliseconds)

    println("서버 IP를 입력하세요.")
    val ip = readln()
    println("서버에 접속하는 중...")

    runCatching {
        val socket = Socket(ip, PORT)
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val output = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)

        println("이름을 입력하세요. (로마자, 숫자, 언더바만 허용)")
        var name: String
        do {
            name = readln()
            if (name.isNotValid()) {
                println("잘못된 이름입니다.")
                continue
            }
            output.println(name)
            if (input.readLine().toBoolean()) break
            println("중복된 이름입니다.")
        } while (true)

        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)

        val keyPair = keyPairGenerator.generateKeyPair()

        val client = Client(name, socket, input, output, keyPair)
        println("서버에 접속했습니다.")
        println("/help 또는 /?를 입력해 명령어 목록을 볼 수 있습니다.")

        launch { InputHandler(client).start() }
        client.start()
    }.onFailure {
        printErr("서버에 접속할 수 없습니다.", it)
        exit(-1)
    }
}