package kr.hahaha98757.securegroupchat.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket

/**
 * 클라이언트와 서버 간의 통신을 처리하는 추상 클래스.
 *
 * @property name 클라이언트의 이름.
 * @property socket 클라이언트와 연결된 소켓.
 * @property input 클라이언트로부터 데이터를 읽어오는 BufferedReader.
 * @property output 클라이언트로 데이터를 보내는 PrintWriter.
 */
abstract class AbstractConnection(
    var name: String,
    private val socket: Socket,
    private val input: BufferedReader,
    private val output: PrintWriter
): AutoCloseable {
    /** 클라이언트가 실행 중인지 나타내는 플래그. */
    @Volatile
    var running = true

    /** 디버그 모드인지 나타내는 플래그. */
    open val isDebug = false

    /** 상대에게 패킷을 전송합니다. */
    fun send(packet: Packet) {
        val encoded = PacketCodec.encode(packet)
        if (running) output.println(encoded)
        if (isDebug) printDebug("Sent to $name: $encoded")
    }

    /**
     * 상대로부터 패킷을 수신하고 처리하는 루프를 시작합니다.
     *
     * 루프는 연결이 유지되는 동안 계속 실행되며, 수신된 패킷을 적절히 처리합니다.
     */
    suspend fun start() {
        var throwable: Throwable? = null
        try {
            while (running) {
                val received = withContext(Dispatchers.IO) { input.readLine() } ?: break // EOF을 만나면 연결 종료
                if (isDebug) printDebug("Received from $name: $received") // 디버그 모드일 경우 수신된 메시지 출력
                val packet = PacketCodec.decode(received) ?: continue // 패킷 디코딩 실패 시 무시
                when (packet) {
                    is SignalPacket -> processSignal(packet)
                    is SecuredPacket -> processSecured(packet)
                    is MessagePacket -> println(packet.message)
                }
            }
        } catch (t: Throwable) {
            if (running) throwable = t // 실행 중 예외 발생 시 throwable에 저장
        } finally {
            /*
            close 호출 중 발생한 예외를 throwable에 추가
            throwable이 이미 있으면 addSuppressed로 추가, 없으면 throwable에 할당
             */
            runCatching { close() }.onFailure { if (throwable != null) throwable.addSuppressed(it) else throwable = it }
            throwable?.let { onException(it) } // 예외가 발생했으면 onException 호출
        }
    }

    /** 연결 중 또는 종료 중 발생한 예외를 처리합니다. */
    protected abstract fun onException(throwable: Throwable)

    /** 수신된 SignalPacket을 처리합니다. */
    protected abstract fun processSignal(packet: SignalPacket)

    /** 수신된 SecuredPacket을 처리합니다. */
    protected abstract fun processSecured(packet: SecuredPacket)

    override fun close() {
        if (!running) return
        running = false
        var isThrown = false // close 중 예외 발생 여부를 나타내는 플래그
        val exception = IOException("Failed to close.") // close 중 예외 발생 시 던질 IOException
        output.close()
        runCatching { input.close() }.onFailure {
            // close 중 예외 발생 시 isThrown을 true로 설정하고 e에 suppressed로 추가
            isThrown = true
            exception.addSuppressed(it)
        }
        runCatching { socket.close() }.onFailure {
            isThrown = true
            exception.addSuppressed(it)
        }
        if (isThrown) throw exception // close 중 예외가 발생했으면 예외를 던짐
    }

    // 이름이 같은 연결은 동일한 연결으로 간주되도록 equals와 hashCode를 재정의
    override fun equals(other: Any?) = other is AbstractConnection && other.name == name
    override fun hashCode() = name.hashCode()
}