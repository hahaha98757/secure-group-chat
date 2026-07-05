package kr.hahaha98757.securegroupchat.common

sealed interface Packet
sealed interface SignalPacket: Packet

/** 일반적인 메시지를 전달하는 패킷. */
data class MessagePacket(val message: String): Packet

/**
 * AES 키로 암호화된 메시지를 전달하는 패킷.
 *
 * @property sender 메시지를 보낸 클라이언트의 이름.
 * @property encryptedMessage 암호화된 메시지.
 */
data class SecuredPacket(val sender: String, val encryptedMessage: String): Packet

/**
 * 클라이언트의 이름을 변경하는 요청을 전달하는 패킷.
 *
 * @property name 변경할 이름.
 */
data class RenamePacket(val name: String): SignalPacket

/**
 * 클라이언트의 이름 변경 요청에 대한 응답을 전달하는 패킷.
 *
 * @property name 변경된 이름.
 * @property accepted 변경이 허용되었는지 여부.
 */
data class RenameResponsePacket(val name: String, val accepted: Boolean): SignalPacket

/** 서버에 존재하는 세션 목록을 요청하는 패킷. */
object SessionListPacket: SignalPacket

/** 현재 세션의 접속자 목록을 요청하는 패킷. */
object UserListPacket: SignalPacket

/** 현재 세션의 접속 요청 목록을 요청하는 패킷. */
object JoinRequestListPacket: SignalPacket

/**
 * 세션을 생성하는 요청을 전달하는 패킷.
 *
 * @property name 생성할 세션의 이름.
 * @property password 세션의 비밀번호. (선택 사항)
 */
data class CreateSessionPacket(val name: String, val password: String? = null): SignalPacket

/** 세션 생성 요청에 대한 응답을 전달하는 패킷. */
data class CreateSessionResponsePacket(val accepted: Boolean): SignalPacket

/** 세션이 준비되었음을 알리는 패킷. */
object SessionReadyPacket: SignalPacket

/**
 * 세션에 접속 요청을 전달하는 패킷.
 *
 * @property client 접속 요청을 보낸 클라이언트의 이름.
 * @property session 접속 요청을 보낸 세션의 이름.
 * @property password 세션의 비밀번호. (선택 사항)
 */
data class JoinRequestPacket(val client: String, val session: String, val password: String? = null): SignalPacket

/**
 * 세션 접속 요청에 대한 결정을 전달하는 패킷.
 *
 * @property client 접속 요청을 보낸 클라이언트의 이름.
 * @property accepted 접속 요청이 허용되었는지 여부.
 */
data class JoinDecision(val client: String, val accepted: Boolean): SignalPacket

/** 세션 접속 요청이 실패했음을 알리는 패킷. */
object JoinFailedPacket: SignalPacket

/** 세션 접속 요청을 취소함을 알리는 패킷. */
object JoinRequestCancelPacket: SignalPacket

/**
 * 세션 키를 요청하는 패킷.
 *
 * @property client 요청을 보낸 클라이언트의 이름.
 * @property encodedPublicKey 요청을 보낸 클라이언트의 공개키를 Base64로 인코딩한 문자열.
 */
data class SessionKeyRequestPacket(val client: String, val encodedPublicKey: String): SignalPacket

/**
 * 세션 키를 전달하는 패킷.
 *
 * @property client 세션 키를 전달할 클라이언트의 이름.
 * @property encryptedKey 세션 키를 RSA로 암호화한 문자열.
 */
data class SessionKeyPacket(val client: String, val encryptedKey: String): SignalPacket

/** 세션에서 특정 클라이언트를 강퇴하는 패킷. */
data class KickPacket(val client: String): SignalPacket

/**
 * 클라이언트가 세션에서 나가고 있음을 알리는 패킷. (클라이언트가 전달 시)
 *
 * 클라이언트를 세션에서 내보내는 패킷. (서버가 전달 시)
 */
object LeavePacket: SignalPacket

/**
 * 클라이언트가 프로그램을 종료하고 있음을 알리는 패킷. (클라이언트가 전달 시)
 *
 * 클라이언트를 종료하는 패킷. (서버가 전달 시)
 */
object ClosePacket: SignalPacket