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

class Client(
    name: String,
    socket: Socket,
    input: BufferedReader,
    output: PrintWriter,
    private var keyPair: KeyPair
): AbstractClient(name, socket, input, output) {
    var session: ClientSession? = null
    var isWaitingJoin = false
    var sessionKey: SecretKey? = null
    
    override fun onException(e: Throwable) {
        printErr("서버와 연결이 끊겼습니다.", e)
        exit(-1)
    }

    override fun processSignal(packet: SignalPacket) {
        when (packet) {
            is RenameResponsePacket -> {
                if (!packet.accepted) println("중복된 이름입니다.")
                else {
                    println("이름을 변경했습니다.")
                    name = packet.name
                }
            }
            is CreateSessionResponsePacket -> {
                if (!packet.accepted) {
                    println("중복된 이름입니다.")
                    session = null
                    isWaitingJoin = false
                } else {
                    println("세션을 생성했습니다.")
                    isWaitingJoin = false
                    val keyGenerator = KeyGenerator.getInstance("AES")
                    keyGenerator.init(256)
                    sessionKey = keyGenerator.generateKey()
                    send(SessionReadyPacket)
                }
            }
            is JoinFailedPacket -> {
                println("세션이 없거나, 비밀번호가 일치하지 않습니다.")
                session = null
                isWaitingJoin = false
            }
            is JoinDecision -> {
                if (packet.accepted) {
                    println("세션 참가 요청이 수락되었습니다.")
                    isWaitingJoin = false
                    println("세션 키를 요청하는 중...")
                    send(SessionKeyRequestPacket(name, Base64.encode(keyPair.public.encoded)))
                } else {
                    println("세션 참가 요청이 거절되었습니다.")
                    session = null
                    isWaitingJoin = false
                }
            }
            is SessionKeyRequestPacket -> {
                val keyBytes = Base64.decode(packet.encodedPublicKey)
                val keyFactory = KeyFactory.getInstance("RSA")
                val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
                val cipher = Cipher.getInstance("RSA")
                cipher.init(Cipher.ENCRYPT_MODE, publicKey)
                val encryptedKey = cipher.doFinal(sessionKey!!.encoded)
                send(SessionKeyPacket(packet.client, Base64.encode(encryptedKey)))
            }
            is SessionKeyPacket -> {
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
            else -> Unit
        }
    }

    override fun processSecured(packet: SecuredPacket) {
        val encryptedBytes = Base64.decode(packet.encryptedMessage)
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, sessionKey)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        val message = String(decryptedBytes)
        println("<${packet.sender}> $message")
    }
}
