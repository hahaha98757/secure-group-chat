package kr.hahaha98757.securegroupchat.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.hahaha98757.securegroupchat.common.*

/** 사용자 입력을 처리하는 클래스. */
class InputHandler {

    /**
     * 사용자 입력을 처리하는 루프를 시작합니다.
     *
     * 이 루프는 프로그램 종료 시까지 무한히 반복합니다.
     */
    suspend fun start(): Nothing {
        while (true) {
            val input = withContext(Dispatchers.IO) { readln() }
            if (!input.startsWith("/")) println("명령어만 입력할 수 있습니다. /help를 입력하여 도움말을 확인하세요.")

            val fullArr = input.substring(1).split(" ") // 앞의 '/'를 제거하고 공백으로 분리
            val command = fullArr[0]
            val args = fullArr.subList(1, fullArr.size)
            processCommand(command, args.toTypedArray())
        }
    }

    /**
     * 명령어를 처리합니다.
     *
     * @param command 명령어 문자열.
     * @param args 명령어 인자 배열.
     */
    private fun processCommand(command: String, args: Array<String>): Unit = when (command) {
        "list" -> { // 세션 또는 사용자 목록 조회 명령어
            if (args.isEmpty()) {
                printCommandUsage("/list </s | /u>")
                return
            }
            if (args[0] == "/s") println("세션 목록: [${ServerSession.sessions.values.joinToString(", ") { it.name }}]")
            else if (args[0] == "/u") println("유저 목록: [${clients.keys.joinToString(", ")}]")
            else printCommandUsage("/list </s | /u>")
        }
        "exit" -> { // 서버 종료 명령어
            println("서버를 종료하는 중...")
            clients.values.forEach {
                it.send(MessagePacket("[server] 서버가 종료됩니다."))
                it.send(ClosePacket)
            }
            exit(0)
        }
        "send" -> { // 메시지 전송 명령어
            if (args.isEmpty()) {
                printCommandUsage("/send <message...>")
                return
            }
            val message = args.joinToString(" ")
            val packet = MessagePacket("[server] $message")
            clients.values.forEach { it.send(packet) }
        }
        "help", "?" -> commandHelp() // 도움말 명령어
        else -> println("알 수 없는 명령어입니다. /help를 입력하여 도움말을 확인하세요.")
    }
}