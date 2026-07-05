package kr.hahaha98757.securegroupchat.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kr.hahaha98757.securegroupchat.common.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.security.KeyPairGenerator
import kotlin.time.Duration.Companion.milliseconds

var debug = false

fun main(args: Array<String>): Unit = runBlocking {
    if ("--debug" in args) {
        printDebug("Debug mode enabled.")
        debug = true
    }
    println("Copyright (c) 2026 hahaha98757 (MIT License)")
    println("Secure Group Chat (client) v1.0.0")
    println("공식 사이트: https://github.com/hahaha98757/secure-group-chat")
    println()
    delay(1000.milliseconds)

    println("서버 IP를 입력하세요.")
    val ip = readln()
    println("서버에 접속하는 중...")

    try {
        val socket = Socket(ip, PORT) // 서버에 접속
        // IO 스트림 생성
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val output = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)

        println("이름을 입력하세요. (로마자, 숫자, 언더바만 허용)")
        var name: String
        // 올바른 이름을 입력할 때까지 반복
        while (true) {
            name = readln() // 이름을 입력 받음
            if (name.isNotValid()) {
                println("잘못된 이름입니다.")
                continue // 잘못된 이름이면 다시 입력 받음
            }
            output.println(name) // 서버에 이름 전송
            if (input.readLine().toBoolean()) break // 서버에서 이름이 중복되지 않았다고 응답하면 반복 종료
            println("중복된 이름입니다.") // 중복된 이름이면 다시 입력 받음
        }

        // RSA 키 쌍 생성
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        val connection = Connection(name, socket, input, output, keyPair)
        println("서버에 접속했습니다.")
        println("/help 또는 /?를 입력해 명령어 목록을 볼 수 있습니다.")

        launch { InputHandler(connection).start() } // 새 코루틴에서 사용자 입력 처리 시작
        connection.start() // 서버와의 통신 시작
    } catch (t: Throwable) {
        printErr("서버에 접속할 수 없습니다.", t)
        exit(-1)
    }
}