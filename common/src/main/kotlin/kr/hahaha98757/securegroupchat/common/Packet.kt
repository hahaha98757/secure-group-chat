package kr.hahaha98757.securegroupchat.common

sealed interface Packet

sealed interface SignalPacket: Packet
data class MessagePacket(val message: String): Packet
data class SecuredPacket(val sender: String, val encryptedMessage: String): Packet

data class RenamePacket(val name: String): SignalPacket
data class RenameResponsePacket(val name: String, val accepted: Boolean): SignalPacket
object SessionListPacket: SignalPacket
object UserListPacket: SignalPacket
object JoinRequestListPacket: SignalPacket
data class CreateSessionPacket(val name: String, val password: String? = null): SignalPacket
data class CreateSessionResponsePacket(val accepted: Boolean): SignalPacket
object SessionReadyPacket: SignalPacket
data class JoinRequestPacket(val client: String, val session: String, val password: String? = null): SignalPacket
data class JoinDecision(val client: String, val accepted: Boolean): SignalPacket
object JoinFailedPacket: SignalPacket
object JoinRequestCancelPacket: SignalPacket
data class SessionKeyRequestPacket(val client: String, val encodedPublicKey: String): SignalPacket
data class SessionKeyPacket(val client: String, val encryptedKey: String): SignalPacket
data class KickPacket(val client: String): SignalPacket
object LeavePacket: SignalPacket
object ClosePacket: SignalPacket