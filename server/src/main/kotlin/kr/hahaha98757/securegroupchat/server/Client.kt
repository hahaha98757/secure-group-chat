package kr.hahaha98757.securegroupchat.server

import kr.hahaha98757.securegroupchat.common.*
import kr.hahaha98757.securegroupchat.server.ServerSession.Companion.sessions
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.Socket

/**
 * 클라이언트와의 연결을 관리하는 클래스.
 *
 * @param name 연결된 클라이언트의 이름.
 * @param socket 클라이언트와의 소켓 연결.
 * @param input 클라이언트로부터의 입력 스트림.
 * @param output 클라이언트로의 출력 스트림.
 */
class Client(name: String, socket: Socket, input: BufferedReader, output: PrintWriter): AbstractConnection(name, socket, input, output) {
    var session: ServerSession? = null // 현재 연결된 세션 정보
    
    init {
        println("$name 님이 서버에 접속했습니다.")
    }

    override val isDebug = debug

    override fun onException(throwable: Throwable) {
        printErr("$name 님의 연결이 끊겼습니다.", throwable)
        // 세션에 참가 중인 경우 세션에서 나가기
        sendOnSession(MessagePacket("${name}님이 세션을 떠났습니다."))
        if (session?.owner == this) session?.remove()
        else session?.leave(this)
        clients -= name
    }

    override fun processSignal(packet: SignalPacket) {
        when (packet) {
            is RenamePacket ->
                if (packet.name !in clients.keys) {
                    // 이름 변경
                    send(RenameResponsePacket(packet.name, true))
                    clients -= name
                    name = packet.name
                    clients[name] = this
                } else send(RenameResponsePacket(name, false)) // 중복된 이름
            is SessionListPacket -> send(MessagePacket("세션 목록: [${sessions.values.joinToString(", ") { it.name }}]"))
            is UserListPacket -> send(MessagePacket("유저 목록: [${session!!.members.values.joinToString(", ") { it.name }}]"))
            is JoinRequestListPacket -> send(MessagePacket("세션 참가 요청 목록: [${session!!.request.joinToString(", ")}]"))
            is CreateSessionPacket ->
                if (packet.name in sessions) send(CreateSessionResponsePacket(false)) // 중복된 세션 이름
                else {
                    session = ServerSession(packet.name, this, packet.password)
                    send(CreateSessionResponsePacket(true))
                }
            is SessionReadyPacket -> session?.ready()
            is JoinRequestPacket -> {
                val session = sessions[packet.session]
                if (session == null) { // 세션이 존재하지 않음
                    send(JoinFailedPacket)
                    return
                }
                if (!session.join(this, packet.password)) send(JoinFailedPacket) // 비밀번호가 틀리거나 이미 세션에 참가 중인 경우
                else session.sendAll(MessagePacket("${packet.client}님이 세션 참가를 요청했습니다."), this)
            }
            is JoinDecision ->
                if (packet.client !in session!!.request) send(MessagePacket("${packet.client}님은 세션 참가 요청 목록에 없습니다."))
                else session!!.acceptOrReject(clients[packet.client]!!, packet.accepted)

            is JoinRequestCancelPacket -> {
                session?.sendAll(MessagePacket("${name}님이 세션 참가 요청을 취소했습니다."), this)
                session?.cancelRequest(this)
            }
            is SessionKeyRequestPacket -> session!!.owner.send(packet) // 주인에게 패킷 전달
            is SessionKeyPacket -> clients[packet.client]?.send(packet) // 요청자에게 패킷 전달
            is LeavePacket -> {
                val session = session!!
                // 주인이면 세션 제거, 아니면 세션에서 나가기
                sendOnSession(MessagePacket("${name}님이 세션을 떠났습니다."))
                if (session.owner == this) session.remove()
                else session.leave(this)
            }
            is KickPacket -> {
                if (session?.owner != this) {
                    send(MessagePacket("세션 주인이 아닙니다."))
                    return
                }
                val targetClient = session!!.members[packet.client]
                if (targetClient == null) {
                    send(MessagePacket("${packet.client}님이 세션에 없습니다."))
                    return
                }
                if (targetClient == this) {
                    send(MessagePacket("자기 자신을 추방할 수 없습니다."))
                    return
                }
                targetClient.send(MessagePacket("세션 주인에 의해 세션에서 추방되었습니다."))
                session!!.leave(targetClient)
                sendOnSession(MessagePacket("${packet.client}님이 세션에서 추방되었습니다."))
            }
            is ClosePacket -> {
                session?.let { // 세션에 참가 중이면 세션에서 나가기
                    sendOnSession(MessagePacket("${name}님이 세션을 떠났습니다."))
                    if (it.owner == this) it.remove()
                    else it.leave(this)
                }
                send(ClosePacket)
                clients -= name
                close()
            }
            else -> return // 그 외의 패킷은 무시
        }
    }

    override fun processSecured(packet: SecuredPacket) = sendOnSession(packet) // 세션의 모든 멤버에게 패킷 전달

    override fun close() {
        if (running) println("$name 님이 서버를 떠났습니다.") // 정상 종료 시 로그 출력
        runCatching { super.close() }.onFailure { // close 중 예외 발생 시 로그 출력
            printErr("$name 님이 서버를 완전히 떠나는데 실패했습니다. (서버 동작에는 영향이 없지만, 잠재적인 문제가 발생할 수 있습니다.)", it)
        }
    }

    /**
     * 자신을 제외한 세션의 모든 멤버에게 패킷을 전송합니다.
     *
     * 세션에 참가 중이지 않으면 아무 동작도 하지 않습니다.
     */
    private fun sendOnSession(packet: Packet) = session?.sendAll(packet, this) ?: Unit
}
