package kr.hahaha98757.securegroupchat.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket

abstract class AbstractClient(
    var name: String,
    private val socket: Socket,
    private val input: BufferedReader,
    private val output: PrintWriter
): AutoCloseable {
    @Volatile
    var running = true

    open val isDebug = false

    fun send(packet: Packet) {
        if (running) output.println(packet.encode())
        if (isDebug) printDebug("Sent to $name: ${packet.encode()}")
    }

    suspend fun start() {
        var throwable: Throwable? = null
        try {
            while (running) {
                val received = withContext(Dispatchers.IO) { input.readLine() } ?: break
                if (isDebug) printDebug("Received from $name: $received")
                val packet = received.decode() ?: continue
                when (packet) {
                    is SignalPacket -> processSignal(packet)
                    is SecuredPacket -> processSecured(packet)
                    is MessagePacket -> println(packet.message)
                }
            }
        } catch (e: Throwable) {
            if (running) throwable = e
        } finally {
            runCatching { close() }.onFailure { if (throwable != null) throwable.addSuppressed(it) else throwable = it }
            throwable?.let { onException(it) }
        }
    }

    protected abstract fun onException(e: Throwable)

    protected abstract fun processSignal(packet: SignalPacket)

    protected abstract fun processSecured(packet: SecuredPacket)

    override fun close() {
        if (!running) return
        running = false
        var isThrown = false
        val e = IOException("Failed to close.")
        output.close()
        runCatching { input.close() }.onFailure {
            isThrown = true
            e.addSuppressed(it)
        }
        runCatching { socket.close() }.onFailure {
            isThrown = true
            e.addSuppressed(it)
        }
        if (isThrown) throw e
    }

    override fun equals(other: Any?) = other is AbstractClient && other.name == name

    override fun hashCode() = name.hashCode()
}