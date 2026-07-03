package kr.hahaha98757.securegroupchat.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.hahaha98757.securegroupchat.common.*

class InputHandler {
    suspend fun start(): Nothing {
        while(true) {
            val input = withContext(Dispatchers.IO) { readln() }
            if (input.startsWith("/")) {
                val fullArr = input.substring(1).split(" ")
                val command = fullArr[0]
                val list = fullArr.subList(1, fullArr.size)
                processCommand(command, list.toTypedArray())
                continue
            }
            println("명령어만 입력할 수 있습니다. /help를 입력하여 도움말을 확인하세요.")
        }
    }

    private fun processCommand(command: String, args: Array<String>) {
        when (command) {
            "list" -> {
                if (args.isEmpty()) {
                    printUsage("/list </s | /u>")
                    return
                }
                if (args[0] == "/s") println("세션 목록: [${ServerSession.sessions.values.joinToString(", ") { it.name }}]")
                else if (args[0] == "/u") println("유저 목록: [${clients.keys.joinToString(", ")}]")
                else printUsage("/list </s | /u>")
            }
            "exit" -> {
                println("서버를 종료하는 중...")
                clients.values.forEach {
                    it.send(MessagePacket("[server] 서버가 종료됩니다."))
                    it.send(ClosePacket)
                }
                exit(0)
            }
            "send" -> {
                if (args.isEmpty()) {
                    printUsage("/send <message...>")
                    return
                }
                val message = args.joinToString(" ")
                val packet = MessagePacket("[server] $message")
                clients.values.forEach { it.send(packet) }
            }
            "help", "?" -> help()
            else -> println("알 수 없는 명령어입니다. /help를 입력하여 도움말을 확인하세요.")
        }

    }
}