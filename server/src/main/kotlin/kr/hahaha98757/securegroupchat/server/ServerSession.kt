package kr.hahaha98757.securegroupchat.server

import kr.hahaha98757.securegroupchat.common.JoinDecision
import kr.hahaha98757.securegroupchat.common.LeavePacket
import kr.hahaha98757.securegroupchat.common.MessagePacket
import kr.hahaha98757.securegroupchat.common.Packet

class ServerSession(val name: String, val owner: Client, val pass: String? = null) {
    companion object {
        val sessions = mutableMapOf<String, ServerSession>()
    }
    private var open = false
    val members = mutableMapOf<String, Client>()
    val request = mutableSetOf<String>()

    init {
        members[owner.name] = owner
    }

    fun open() {
        open = true
        sessions[name] = this
    }

    fun join(client: Client, pass: String? = null): Boolean {
        if (client == owner) return false
        if (this.pass != null && this.pass != pass) return false

        request += client.name
        client.session = this
        return true
    }

    fun acceptOrReject(client: Client, accept: Boolean) {
        request.find { it == client.name }?.let {
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

    fun leave(client: Client) {
        members -= client.name
        request -= client.name
        client.send(LeavePacket)
    }

    fun cancelRequest(client: Client) {
        request -= client.name
    }

    fun remove() {
        members.values.forEach { it.send(LeavePacket) }
        members.clear()
        request.clear()
        sessions -= this.name
    }

    fun sendAll(packet: Packet, vararg excludes: Client) {
        members.values.forEach { if (it !in excludes) it.send(packet) }
    }

    override fun equals(other: Any?) = other is ServerSession && other.name == name
    override fun hashCode() = name.hashCode()
}