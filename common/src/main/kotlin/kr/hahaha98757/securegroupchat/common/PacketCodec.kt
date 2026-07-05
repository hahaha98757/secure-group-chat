package kr.hahaha98757.securegroupchat.common

object PacketCodec {

    /**
     * 패킷을 문자열로 인코딩합니다.
     *
     * @param packet 인코딩할 패킷.
     * @return 인코딩된 문자열.
     */
    fun encode(packet: Packet) = when (packet) {
        is MessagePacket -> "message;${packet.message}"
        is SecuredPacket -> "secured;${packet.sender}:${packet.encryptedMessage}"
        is RenamePacket -> "signal;rename:${packet.name}"
        is RenameResponsePacket -> "signal;renameResponse:${packet.name}:${packet.accepted}"
        is SessionListPacket -> "signal;sessionList"
        is UserListPacket -> "signal;userList"
        is JoinRequestListPacket -> "signal;joinRequestList"
        is CreateSessionPacket -> "signal;createSession:${packet.name}:${packet.password ?: ""}"
        is CreateSessionResponsePacket -> "signal;createSessionResponse:${packet.accepted}"
        is SessionReadyPacket -> "signal;sessionReady"
        is JoinRequestPacket -> "signal;joinRequest:${packet.client}:${packet.session}:${packet.password ?: ""}"
        is JoinDecision -> "signal;joinDecision:${packet.client}:${packet.accepted}"
        is JoinFailedPacket -> "signal;joinFailed"
        is JoinRequestCancelPacket -> "signal;joinRequestCancel"
        is SessionKeyRequestPacket -> "signal;sessionKeyRequest:${packet.client}:${packet.encodedPublicKey}"
        is SessionKeyPacket -> "signal;sessionKey:${packet.client}:${packet.encryptedKey}"
        is KickPacket -> "signal;kick:${packet.client}"
        is LeavePacket -> "signal;leave"
        is ClosePacket -> "signal;close"
    }

    /**
     * 문자열을 패킷으로 디코딩합니다.
     *
     * @param string 디코딩할 문자열.
     * @return 디코딩된 패킷, 디코딩할 수 없는 경우 null.
     */
    fun decode(string: String): Packet? {
        val format = string.substringBefore(";")
        val args = string.substringAfter(";").split(":")

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

    /**
     * 컬렉션의 요소들을 지정된 인덱스부터 연결하여 문자열로 반환합니다.
     *
     * @param index 시작 인덱스.
     * @param delimiter 구분자.
     * @return 연결된 문자열.
     */
    private fun Collection<String>.joinAfter(index: Int, delimiter: String = ":") = this.drop(index).joinToString(delimiter)
}