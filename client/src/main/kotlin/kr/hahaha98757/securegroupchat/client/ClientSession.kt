package kr.hahaha98757.securegroupchat.client

/**
 * 세션을 나타내는 클래스입니다.
 *
 * @property name 세션 이름.
 * @property isOwner 세션 소유자인지 여부.
 */
class ClientSession(val name: String, val isOwner: Boolean) {
    // 이름이 같은 세션은 동일한 세션으로 간주되도록 equals와 hashCode를 재정의
    override fun equals(other: Any?) = other is ClientSession && other.name == name
    override fun hashCode() = name.hashCode()
}
