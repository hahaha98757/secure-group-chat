package kr.hahaha98757.securegroupchat.server

import kr.hahaha98757.securegroupchat.common.*
import kr.hahaha98757.securegroupchat.server.ServerSession.Companion.sessions
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.Socket

class Client(name: String, socket: Socket, input: BufferedReader, output: PrintWriter): AbstractClient(name, socket, input, output) {
    var session: ServerSession? = null
    
    init {
        println("$name 님이 서버에 접속했습니다.")
    }

    override val isDebug = debug

    override fun onException(e: Throwable) {
        printErr("$name 님의 연결이 끊겼습니다.", e)
        sendOnSession(MessagePacket("${name}님이 세션을 떠났습니다."))
        if (session?.owner == this) session?.remove()
        else session?.leave(this)
        clients -= name
    }

    override fun processSignal(packet: SignalPacket) {
        when (packet) {
            is RenamePacket ->
                if (packet.name !in clients.keys) {
                    send(RenameResponsePacket(packet.name, true))
                    clients -= name
                    name = packet.name
                    clients[name] = this
                } else send(RenameResponsePacket(name, false))
            is SessionListPacket -> send(MessagePacket("세션 목록: [${sessions.values.joinToString(", ") { it.name }}]"))
            is UserListPacket -> send(MessagePacket("유저 목록: [${session!!.members.values.joinToString(", ") { it.name }}]"))
            is JoinRequestListPacket -> send(MessagePacket("세션 참가 요청 목록: ${session!!.request.joinToString(", ")}"))
            is CreateSessionPacket ->
                if (packet.name in sessions) send(CreateSessionResponsePacket(false))
                else {
                    session = ServerSession(packet.name, this, packet.password)
                    send(CreateSessionResponsePacket(true))
                }
            is SessionReadyPacket -> session?.open()
            is JoinRequestPacket -> {
                val session = sessions[packet.session]
                if (session == null) {
                    send(JoinFailedPacket)
                    return
                }
                if (!session.join(this, packet.password)) send(JoinFailedPacket)
                else session.sendAll(MessagePacket("${packet.client}님이 세션 참가를 요청했습니다."), this)
            }
            is JoinDecision ->
                if (packet.client !in session!!.request) send(MessagePacket("${packet.client}님은 세션 참가 요청 목록에 없습니다."))
                else session!!.acceptOrReject(clients[packet.client]!!, packet.accepted)

            is JoinRequestCancelPacket -> {
                session?.sendAll(MessagePacket("${name}님이 세션 참가 요청을 취소했습니다."), this)
                session?.cancelRequest(this)
            }
            is SessionKeyRequestPacket -> session!!.owner.send(packet)
            is SessionKeyPacket -> clients[packet.client]?.send(packet)
            is LeavePacket -> {
                val session = session!!
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
                session?.let {
                    sendOnSession(MessagePacket("${name}님이 세션을 떠났습니다."))
                    if (it.owner == this) it.remove()
                    else it.leave(this)
                }
                send(ClosePacket)
                clients -= name
                close()
            }
            else -> Unit
        }
    }

    override fun processSecured(packet: SecuredPacket) = sendOnSession(packet)

    override fun close() {
        if (running) println("$name 님이 서버를 떠났습니다.")
        runCatching { super.close() }.onFailure {
            printErr("$name 님이 서버를 완전히 떠나는데 실패했습니다. (서버 동작에는 영향이 없지만, 잠재적인 문제가 발생할 수 있습니다.)", it)
        }
    }

    private fun sendOnSession(packet: Packet) {
        session?.sendAll(packet, this)
    }
}
