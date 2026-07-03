package kr.hahaha98757.securegroupchat.client

class ClientSession(val name: String, val isOwner: Boolean) {
    override fun equals(other: Any?) = other is ClientSession && other.name == name
    override fun hashCode() = name.hashCode()
}
