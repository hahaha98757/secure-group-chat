package kr.hahaha98757.securegroupchat.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.hahaha98757.securegroupchat.common.*
import javax.crypto.Cipher
import kotlin.io.encoding.Base64

/**
 * 사용자 입력을 처리하는 클래스.
 *
 * @param connection 서버와 통신할 [Connection] 객체.
 */
class InputHandler(private val connection: Connection) {

    /**
     * 사용자 입력을 처리하는 루프를 시작합니다.
     *
     * [Connection] 객체가 실행 중인 동안 사용자 입력을 계속해서 읽고 처리합니다.
     */
    suspend fun start() {
        while (connection.running) {
            val input = withContext(Dispatchers.IO) { readln() } // withContext를 사용하여 스레드 블로킹을 방지하며 입력 받음
            if (connection.isWaitingJoin) { // 세션 참가 대기 중인 경우
                if (input == "/leave") {
                    connection.isWaitingJoin = false
                    connection.session = null
                    println("세션 참가를 취소했습니다.")
                    connection.send(JoinRequestCancelPacket)
                } else println("세션에 접속 대기 중입니다. 떠나려면 /leave를 입력하세요.")
                continue
            }
            if (input.startsWith("/")) { // 명령어 처리
                val fullArr = input.substring(1).split(" ") // 앞의 '/'를 제거하고 공백으로 분리
                // 명령어와 인자를 분리
                val command = fullArr[0]
                val args = fullArr.subList(1, fullArr.size)
                processCommand(command, args.toTypedArray())
                continue
            }
            // 일반 메시지 처리
            if (connection.session == null) {
                println("세션에 접속하지 않은 상태에서는 메시지를 보낼 수 없습니다.")
                continue
            }
            if (connection.sessionKey == null) {
                println("세션 키가 설정되지 않았습니다. 잠시만 기다려주세요.")
                continue
            }
            // AES 암호화 및 Base64 인코딩 후 전송
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, connection.sessionKey)
            val encrypted = cipher.doFinal(input.toByteArray())
            val encoded = Base64.encode(encrypted)
            println("<${connection.name}> $input")
            connection.send(SecuredPacket(connection.name, encoded))
        }
    }

    /**
     * 명령어를 처리합니다.
     *
     * @param command 명령어 문자열.
     * @param args 명령어 인자 배열.
     */
    private fun processCommand(command: String, args: Array<String>): Unit = when (command) {
        "rename" -> { // 이름 변경 명령어
            if (connection.session != null) {
                println("세션에 접속 중에는 이름을 변경할 수 없습니다.")
                return
            }
            if (args.isEmpty()) {
                printCommandUsage("/rename <name>")
                return
            }
            val name = args[0]
            if (name.isNotValid()) {
                println("잘못된 이름입니다.")
                return
            }
            connection.send(RenamePacket(name))
        }
        "list" -> { // 세션 또는 사용자 목록 조회 명령어
            if (args.isEmpty()) {
                printCommandUsage("/list </s | /u>")
                return
            }
            if (args[0] == "/s") connection.send(SessionListPacket)
            else if (connection.session == null) println("세션에 접속하지 않은 상태입니다.")
            else if (args[0] == "/u") connection.send(UserListPacket)
            else printCommandUsage("/list </s | /u>")
        }
        "create" -> { // 세션 생성 명령어
            if (connection.session != null) {
                println("이미 세션에 접속 중입니다.")
                return
            }
            if (args.isEmpty()) {
                printCommandUsage("/create <name> [password]")
                return
            }
            if (args[0].isNotValid() || args.getOrNull(1)?.isNotValid() == true) {
                println("잘못된 세션 이름 또는 비밀번호입니다.")
                return
            }
            connection.send(CreateSessionPacket(args[0], args.getOrNull(1)))
            connection.session = ClientSession(args[0], true) // 세션 생성 후 세션에 접속 상태로 변경
            connection.isWaitingJoin = true // 이름 중복을 확인하기 위해 세션 참가 대기 상태로 변경
        }
        "join" -> { // 세션 참가 명령어
            if (connection.session != null) {
                println("이미 세션에 접속 중입니다.")
                return
            }
            if (args.isEmpty()) {
                printCommandUsage("/join <name> [password]")
                return
            }
            if (args[0].isNotValid() || args.getOrNull(1)?.isNotValid() == true) {
                println("잘못된 세션 이름 또는 비밀번호입니다.")
                return
            }
            connection.send(JoinRequestPacket(connection.name, args[0], args.getOrNull(1)))
            connection.session = ClientSession(args[0], false) // 세션 참가 요청 후 세션에 접속 상태로 변경
            connection.isWaitingJoin = true // 주인의 승인을 기다리기 위해 세션 참가 대기 상태로 변경
            println("세션 참가를 요청했습니다.")
        }
        "request" -> { // 세션 참가 요청 처리 명령어
            if (connection.session?.isOwner != true) {
                println("세션 주인만이 참가 요청을 처리할 수 있습니다.")
                return
            }
            if (args.isEmpty()) { // 참가 요청 목록 조회
                connection.send(JoinRequestListPacket)
                return
            }
            if (args.size != 2) {
                printCommandUsage("/request [/a | /r] [name]")
                return
            }
            // 인자를 이용해 참가 요청 수락 또는 거부 여부 결정
            val accepted = if (args[0] == "/a") true else if (args[0] == "/r") false else {
                printCommandUsage("/request [/a | /r] [name]")
                return
            }
            connection.send(JoinDecision(args[1], accepted)) // 참가 요청 수락 또는 거부 패킷 전송
        }
        "leave" -> { // 세션 떠나기 명령어
            if (connection.session == null) {
                println("세션에 접속하지 않은 상태입니다.")
                return
            }
            connection.send(LeavePacket)
            println("세션을 떠나는 중...")
        }
        "kick" -> { // 세션에서 사용자 추방 명령어
            if (connection.session?.isOwner != true) {
                println("세션 주인이 아닙니다.")
                return
            }
            if (args.isEmpty()) {
                printCommandUsage("/kick <name>")
                return
            }
            connection.send(KickPacket(args[0]))
        }
        "exit" -> { // 클라이언트 종료 명령어
            println("클라이언트를 종료하는 중...")
            connection.send(ClosePacket)
        }
        "help", "?" -> commandHelp() // 도움말 명령어
        else -> println("알 수 없는 명령어입니다. /help를 입력하여 도움말을 확인하세요.")
    }
}