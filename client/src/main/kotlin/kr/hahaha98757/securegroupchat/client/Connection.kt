package kr.hahaha98757.securegroupchat.client

import kr.hahaha98757.securegroupchat.common.*
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

/**
 * 서버와의 연결을 관리하는 클래스.
 *
 * @param name 연결된 클라이언트의 이름.
 * @param socket 서버와의 소켓 연결.
 * @param input 서버로부터의 입력 스트림.
 * @param output 서버로의 출력 스트림.
 * @param keyPair 클라이언트의 RSA 키 쌍.
 */
class Connection(
    name: String,
    socket: Socket,
    input: BufferedReader,
    output: PrintWriter,
    private var keyPair: KeyPair
): AbstractConnection(name, socket, input, output) {
    var session: ClientSession? = null // 현재 연결된 세션 정보
    var isWaitingJoin = false // 세션 참가 대기 상태 플래그
    var sessionKey: SecretKey? = null // 세션에서 사용할 AES 키

    override val isDebug = debug

    override fun onException(throwable: Throwable) {
        printErr("서버와 연결이 끊겼습니다.", throwable)
        exit(-1)
    }

    override fun processSignal(packet: SignalPacket) {
        when (packet) {
            is RenameResponsePacket ->
                if (!packet.accepted) println("중복된 이름입니다.")
                else {
                    println("이름을 변경했습니다.")
                    name = packet.name
                }
            is CreateSessionResponsePacket ->
                if (!packet.accepted) {
                    println("중복된 이름입니다.")
                    // 세션 생성 취소
                    session = null
                    isWaitingJoin = false
                } else {
                    println("세션을 생성했습니다.")
                    isWaitingJoin = false // 세션 참가 대기 상태 해제
                    // 세션 키(AES 키) 생성
                    val keyGenerator = KeyGenerator.getInstance("AES")
                    keyGenerator.init(256)
                    sessionKey = keyGenerator.generateKey()
                    send(SessionReadyPacket)
                }
            is JoinFailedPacket -> {
                println("세션이 없거나, 비밀번호가 일치하지 않습니다.")
                // 세션 참가 취소
                session = null
                isWaitingJoin = false
            }
            is JoinDecision ->
                if (packet.accepted) {
                    println("세션 참가 요청이 수락되었습니다.")
                    isWaitingJoin = false // 세션 참가 대기 상태 해제
                    println("세션 키를 요청하는 중...")
                    send(SessionKeyRequestPacket(name, Base64.encode(keyPair.public.encoded)))
                } else {
                    println("세션 참가 요청이 거절되었습니다.")
                    // 세션 참가 취소
                    session = null
                    isWaitingJoin = false
                }
            is SessionKeyRequestPacket -> {
                //공개 키 디코딩
                val keyBytes = Base64.decode(packet.encodedPublicKey)
                val keyFactory = KeyFactory.getInstance("RSA")
                val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
                // 세션 키를 공개 키로 암호화하여 전송
                val cipher = Cipher.getInstance("RSA")
                cipher.init(Cipher.ENCRYPT_MODE, publicKey)
                val encryptedKey = cipher.doFinal(sessionKey!!.encoded)
                send(SessionKeyPacket(packet.client, Base64.encode(encryptedKey)))
            }
            is SessionKeyPacket -> {
                // 세션 키를 복호화하여 저장
                val encryptedKeyBytes = Base64.decode(packet.encryptedKey)
                val cipher = Cipher.getInstance("RSA")
                cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
                val decryptedKeyBytes = cipher.doFinal(encryptedKeyBytes)
                sessionKey = SecretKeySpec(decryptedKeyBytes, "AES")
                println("세션 키를 받았습니다.")
            }
            is LeavePacket -> {
                println("세션을 떠났습니다.")
                session = null
                isWaitingJoin = false
                sessionKey = null
            }
            is ClosePacket -> {
                println("프로그램을 종료합니다.")
                close()
                exit(0)
            }
            else -> return // 기타 패킷은 무시
        }
    }

    override fun processSecured(packet: SecuredPacket) {
        // 세션 키로 복호화하여 메시지 출력
        val encryptedBytes = Base64.decode(packet.encryptedMessage)
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, sessionKey)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        val message = String(decryptedBytes)
        println("<${packet.sender}> $message")
    }
}
