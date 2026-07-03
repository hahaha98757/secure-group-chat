package kr.hahaha98757.securegroupchat.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.hahaha98757.securegroupchat.common.*
import javax.crypto.Cipher
import kotlin.io.encoding.Base64

class InputHandler(private val client: Client) {
    suspend fun start(): Nothing {
        while(true) {
            val input = withContext(Dispatchers.IO) { readln() }
            if (client.isWaitingJoin) {
                if (input == "/leave") {
                    client.isWaitingJoin = false
                    client.session = null
                    println("세션 참가를 취소했습니다.")
                    client.send(JoinRequestCancelPacket)
                } else println("세션에 접속 대기 중입니다. 떠나려면 /leave를 입력하세요.")
                continue
            }
            if (input.startsWith("/")) {
                val fullArr = input.substring(1).split(" ")
                val command = fullArr[0]
                val list = fullArr.subList(1, fullArr.size)
                processCommand(command, list.toTypedArray())
                continue
            }
            if (client.session == null) {
                println("세션에 접속하지 않은 상태에서는 메시지를 보낼 수 없습니다.")
                continue
            }
            if (client.sessionKey == null) {
                println("세션 키가 설정되지 않았습니다. 잠시만 기다려주세요.")
                continue
            }
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, client.sessionKey)
            val encrypted = cipher.doFinal(input.toByteArray())
            val encoded = Base64.encode(encrypted)
            println("<${client.name}> $input")
            client.send(SecuredPacket(client.name, encoded))
        }
    }

    private fun processCommand(command: String, args: Array<String>) {
        when (command) {
            "rename" -> {
                if (client.session != null) {
                    println("세션에 접속 중에는 이름을 변경할 수 없습니다.")
                    return
                }
                if (args.isEmpty()) {
                    printUsage("/rename <name>")
                    return
                }
                val name = args[0]
                if (name.isNotValid()) {
                    println("잘못된 이름입니다.")
                    return
                }
                client.send(RenamePacket(name))
            }
            "list" -> {
                if (args.isEmpty()) {
                    printUsage("/list </s | /u>")
                    return
                }
                if (args[0] == "/s") client.send(SessionListPacket)
                else if (client.session == null) println("세션에 접속하지 않은 상태입니다.")
                else if (args[0] == "/u") client.send(UserListPacket)
                else printUsage("/list </s | /u>")
            }
            "create" -> {
                if (client.session != null) {
                    println("이미 세션에 접속 중입니다.")
                    return
                }
                if (args.isEmpty()) {
                    printUsage("/create <name> [password]")
                    return
                }
                if (args[0].isNotValid() || args.getOrNull(1)?.isNotValid() == true) {
                    println("잘못된 세션 이름 또는 비밀번호입니다.")
                    return
                }
                client.send(CreateSessionPacket(args[0], args.getOrNull(1)))
                client.session = ClientSession(args[0], true)
                client.isWaitingJoin = true
            }
            "join" -> {
                if (client.session != null) {
                    println("이미 세션에 접속 중입니다.")
                    return
                }
                if (args.isEmpty()) {
                    printUsage("/join <name> [password]")
                    return
                }
                if (args[0].isNotValid() || args.getOrNull(1)?.isNotValid() == true) {
                    println("잘못된 세션 이름 또는 비밀번호입니다.")
                    return
                }
                client.send(JoinRequestPacket(client.name, args[0], args.getOrNull(1)))
                client.session = ClientSession(args[0], false)
                client.isWaitingJoin = true
                println("세션 참가를 요청했습니다.")
            }
            "request" -> {
                if (client.session?.isOwner != true) {
                    println("세션 주인만이 참가 요청을 처리할 수 있습니다.")
                    return
                }
                if (args.isEmpty()) {
                    client.send(JoinRequestListPacket)
                    return
                }
                if (args.size != 2) {
                    printUsage("/request [/a | /r] [name]")
                    return
                }
                val accepted = if (args[0] == "/a") true else if (args[0] == "/r") false else {
                    printUsage("/request [/a | /r] [name]")
                    return
                }
                client.send(JoinDecision(args[1], accepted))
            }
            "leave" -> {
                if (client.session == null) {
                    println("세션에 접속하지 않은 상태입니다.")
                    return
                }
                client.send(LeavePacket)
                println("세션을 떠나는 중...")
            }
            "kick" -> {
                if (client.session?.isOwner != true) {
                    println("세션 주인이 아닙니다.")
                    return
                }
                if (args.isEmpty()) {
                    printUsage("/kick <name>")
                    return
                }
                client.send(KickPacket(args[0]))
            }
            "exit" -> {
                println("클라이언트를 종료하는 중...")
                client.send(ClosePacket)
            }
            "help", "?" -> help()
            else -> println("알 수 없는 명령어입니다. /help를 입력하여 도움말을 확인하세요.")
        }
    }
}