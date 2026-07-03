package kr.hahaha98757.securegroupchat.server

import kotlinx.coroutines.*
import kr.hahaha98757.securegroupchat.common.PORT
import kr.hahaha98757.securegroupchat.common.exit
import kr.hahaha98757.securegroupchat.common.printDebug
import kr.hahaha98757.securegroupchat.common.printErr
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import kotlin.time.Duration.Companion.milliseconds

val clients = mutableMapOf<String, Client>()
var debug = false

fun main(args: Array<String>) = runBlocking {
    if (args.contains("--debug")) {
        printDebug("Debug mode enabled.")
        debug = true
    }

    println("Copyright (c) 2026 hahaha98757 (MIT License)")
    println("Secure Group Chat (server) v1.0.0")
    println("공식 사이트: https://github.com/hahaha98757/secure-group-chat")
    println()
    delay(1000.milliseconds)

    println("서버를 여는 중...")
    val serverSocket = runCatching { ServerSocket(PORT) }.getOrElse {
        printErr("서버를 여는데 실패했습니다.", it)
        exit(-1)
    }

    println("서버를 열었습니다.")
    println("/help 또는 /?를 입력해 명령어 목록을 볼 수 있습니다.")
    launch { InputHandler().start() }

    while (true) try {
        val socket = withContext(Dispatchers.IO) { serverSocket.accept() }
        launch {
            try {
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)

                var name: String
                do {
                    name = withContext(Dispatchers.IO) { input.readLine() }
                    if (name in clients.keys) {
                        output.println(false)
                        continue
                    } else output.println(true)
                    break
                } while (true)

                Client(name, socket, input, output).also { clients[name] = it }.start()
            } catch (e: Exception) {
                printErr("클라이언트와의 연결 중 오류가 발생했습니다.", e)
            }
        }
    } catch (e: Exception) {
        printErr("서버에서 오류가 발생했습니다.", e)
    }
}