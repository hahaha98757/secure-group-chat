package kr.hahaha98757.securegroupchat.common

fun Packet.encode() = when (this) {
    is MessagePacket -> "message;${this.message}"
    is SecuredPacket -> "secured;${this.sender}:${this.encryptedMessage}"
    is RenamePacket -> "signal;rename:${this.name}"
    is RenameResponsePacket -> "signal;renameResponse:${this.name}:${this.accepted}"
    is SessionListPacket -> "signal;sessionList"
    is UserListPacket -> "signal;userList"
    is JoinRequestListPacket -> "signal;joinRequestList"
    is CreateSessionPacket -> "signal;createSession:${this.name}:${this.password ?: ""}"
    is CreateSessionResponsePacket -> "signal;createSessionResponse:${this.accepted}"
    is SessionReadyPacket -> "signal;sessionReady"
    is JoinRequestPacket -> "signal;joinRequest:${this.client}:${this.session}:${this.password ?: ""}"
    is JoinDecision -> "signal;joinDecision:${this.client}:${this.accepted}"
    is JoinFailedPacket -> "signal;joinFailed"
    is JoinRequestCancelPacket -> "signal;joinRequestCancel"
    is SessionKeyRequestPacket -> "signal;sessionKeyRequest:${this.client}:${this.encodedPublicKey}"
    is SessionKeyPacket -> "signal;sessionKey:${this.client}:${this.encryptedKey}"
    is KickPacket -> "signal;kick:${this.client}"
    is LeavePacket -> "signal;leave"
    is ClosePacket -> "signal;close"
}

fun String.decode(): Packet? {
    val format = this.substringBefore(";")
    val args = this.substringAfter(";").split(":")

    if (format == "message") return MessagePacket(args.joinAfter(0))
    else if (format == "secured") return SecuredPacket(args[0], args.joinAfter(1))

    val signal = args[0]
    val signalArgs = args.drop(1)
    return when (signal) {
        "rename" -> RenamePacket(signalArgs.joinAfter(0))
        "renameResponse" -> RenameResponsePacket(signalArgs[0], signalArgs[1].toBoolean())
        "sessionList" -> SessionListPacket
        "userList" -> UserListPacket
        "joinRequestList" -> JoinRequestListPacket
        "createSession" -> CreateSessionPacket(signalArgs[0], signalArgs.getOrNull(1))
        "createSessionResponse" -> CreateSessionResponsePacket(signalArgs[0].toBoolean())
        "sessionReady" -> SessionReadyPacket
        "joinRequest" -> JoinRequestPacket(signalArgs[0], signalArgs[1], signalArgs.getOrNull(2))
        "joinDecision" -> JoinDecision(signalArgs[0], signalArgs[1].toBoolean())
        "joinFailed" -> JoinFailedPacket
        "joinRequestCancel" -> JoinRequestCancelPacket
        "sessionKeyRequest" -> SessionKeyRequestPacket(signalArgs[0], signalArgs.joinAfter(1))
        "sessionKey" -> SessionKeyPacket(signalArgs[0], signalArgs.joinAfter(1))
        "kick" -> KickPacket(signalArgs[0])
        "leave" -> LeavePacket
        "close" -> ClosePacket
        else -> null
    }
}

private fun Collection<String>.joinAfter(index: Int, delimiter: String = ":") = this.drop(index).joinToString(delimiter)