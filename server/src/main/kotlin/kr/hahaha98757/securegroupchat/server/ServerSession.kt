package kr.hahaha98757.securegroupchat.server

import kr.hahaha98757.securegroupchat.common.JoinDecision
import kr.hahaha98757.securegroupchat.common.LeavePacket
import kr.hahaha98757.securegroupchat.common.MessagePacket
import kr.hahaha98757.securegroupchat.common.Packet
import kr.hahaha98757.securegroupchat.server.ServerSession.Companion.sessions

/**
 * 세션을 나타내는 클래스입니다.
 *
 * @property name 세션 이름.
 * @property owner 세션 소유자.
 * @property pass 세션 비밀번호. null이면 비밀번호가 없는 세션입니다.
 */
class ServerSession(val name: String, val owner: Client, val pass: String? = null) {
    companion object {
        /** 만들어진 세션 목록 (키: 이름, 값: 세션 객체) */
        val sessions = mutableMapOf<String, ServerSession>()
    }

    /** 세션에 참여한 클라이언트 목록 (키: 이름, 값: 클라이언트 객체) */
    val members = mutableMapOf<String, Client>()

    /** 세션 참가 요청 목록 (요청한 클라이언트 이름) */
    val request = mutableSetOf<String>()

    init {
        members[owner.name] = owner // 세션 소유자는 자동으로 세션에 참여
    }

    /** 세션을 [sessions]에 추가하여 다른 클라이언트가 세션에 참가할 수 있도록 합니다. */
    fun ready() {
        sessions[name] = this
    }

    /**
     * 클라이언트가 세션에 참가 요청을 합니다.
     *
     * @param client 참가 요청을 하는 클라이언트.
     * @param pass 세션 비밀번호.
     * @return 비밀번호가 맞고, 이미 세션에 참여 중이 아니면 true, 그렇지 않으면 false.
     */
    fun join(client: Client, pass: String? = null): Boolean {
        if (client in members.values) return false
        if (this.pass != null && this.pass != pass) return false

        request += client.name
        client.session = this
        return true
    }

    /**
     * 클라이언트의 세션 참가 요청을 수락하거나 거절합니다.
     *
     * @param client 참가 요청을 하는 클라이언트.
     * @param accept 참가 요청을 수락할지 여부.
     */
    fun acceptOrReject(client: Client, accept: Boolean) {
        request.find { it == client.name }?.let { // 참가 요청이 존재하는 경우에만 처리
            request -= it
            if (accept) {
                members[client.name] = client
                client.session = this
                client.send(JoinDecision(client.name, true))
                sendAll(MessagePacket("${client.name}님이 세션에 참가했습니다."), client)
            } else {
                client.send(JoinDecision(client.name, false))
                sendAll(MessagePacket("${client.name}님의 세션 참가 요청이 거절되었습니다."), client)
            }
        }
    }

    /** 클라이언트를 세션에서 내보냅니다. */
    fun leave(client: Client) {
        members -= client.name
        request -= client.name
        client.send(LeavePacket)
    }

    /** 클라이언트의 세션 참가 요청을 취소합니다. */
    fun cancelRequest(client: Client) {
        request -= client.name
        client.session = null
    }

    /** 세션을 제거합니다. */
    fun remove() {
        members.values.forEach { it.send(LeavePacket) } // 세션에 참여한 모든 클라이언트를 세션에서 내보냄
        members.clear()
        request.clear()
        sessions -= this.name
    }

    /**
     * 세션에 참여한 모든 클라이언트에게 패킷을 전송합니다.
     *
     * @param packet 전송할 패킷.
     * @param excludes 패킷을 전송하지 않을 클라이언트들.
     */
    fun sendAll(packet: Packet, vararg excludes: Client) {
        members.values.forEach { if (it !in excludes) it.send(packet) }
    }

    // 이름이 같은 세션은 동일한 세션으로 간주되도록 equals와 hashCode를 재정의
    override fun equals(other: Any?) = other is ServerSession && other.name == name
    override fun hashCode() = name.hashCode()
}