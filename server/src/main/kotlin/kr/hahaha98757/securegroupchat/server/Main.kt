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

/** 존재하는 클라이언트 목록 (키: 이름, 값: 클라이언트 객체) */
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
    launch { InputHandler().start() } // 새 코루틴에서 사용자 입력 처리 시작

    while (true) try { // 클라이언트 접속 처리 루프
        val socket = withContext(Dispatchers.IO) { serverSocket.accept() } // 클라이언트 접속 대기
        launch { // 새 코루틴에서 클라이언트 접속 처리 및 통신 시작
            try {
                // IO 스트림 생성
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)

                var name: String
                while (true) { // 올바른 이름을 받을 때까지 반복
                    name = withContext(Dispatchers.IO) { input.readLine() } // 클라이언트로부터 이름을 받음
                    if (name in clients.keys) {
                        // 이미 존재하는 이름이면 false를 보내고 다시 이름을 받음
                        output.println(false)
                        continue
                    } else output.println(true) // 사용 가능한 이름이면 true를 보내고 반복 종료
                    break
                }

                // 클라이언트 객체 생성, clients 맵에 추가, 통신 시작
                Client(name, socket, input, output).also { clients[name] = it }.start()
            } catch (e: Exception) {
                printErr("클라이언트와의 연결 중 오류가 발생했습니다.", e)
            }
        }
    } catch (e: Exception) {
        printErr("서버에서 오류가 발생했습니다.", e)
    }
}