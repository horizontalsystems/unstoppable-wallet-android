package bitcoin.wallet.kit.network

import bitcoin.walllet.kit.common.constant.BitcoinConstants
import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.network.MessageSender
import bitcoin.walllet.kit.network.PeerListener
import bitcoin.walllet.kit.network.message.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

class Peer(val host: String, private val listener: PeerListener) : Thread(), MessageSender {

    private val log = LoggerFactory.getLogger(Peer::class.java)
    private val sendingQueue: BlockingQueue<Message> = ArrayBlockingQueue(100)
    private val sock = Socket()

    @Volatile
    private var isRunning = false

    @Volatile
    private var timeout: Long = 0
    private val isTimeout: Boolean
        get() = System.currentTimeMillis() > this.timeout

    // initialize:
    init {
        isDaemon = true
    }

    override fun run() {
        isRunning = true
        // connect:
        try {
            sock.connect(InetSocketAddress(host, BitcoinConstants.PORT), 10000)
            sock.soTimeout = 10000

            val input = sock.getInputStream()
            val output = sock.getOutputStream()

            log.info("Socket $host connected.")
            setTimeout(60000)

            // add version message to send automatically:
            sendMessage(VersionMessage(0, sock.inetAddress))
            // loop:
            while (isRunning) {
                if (isTimeout) {
                    log.info("Timeout!")
                    break
                }

                // try get message to send:
                val msg = sendingQueue.poll(1, TimeUnit.SECONDS)
                if (isRunning && msg != null) {
                    // send message:
                    log.info("=> " + msg.toString())
                    output.write(msg.toByteArray())
                }

                // try receive message:
                if (isRunning && input.available() > 0) {
                    val inputStream = BitcoinInput(input)
                    val parsedMsg = Message.Builder.parseMessage<Message>(inputStream)

                    log.info("<= $parsedMsg")

                    when (parsedMsg) {
                        is PingMessage -> sendMessage(PongMessage(parsedMsg.nonce))
                        is VersionMessage -> sendMessage(VerAckMessage())
                        is VerAckMessage -> listener.connected(host)
                        else -> listener.onMessage(this, parsedMsg)
                    }
                }
            }

            listener.disconnected(this, null)
        } catch (e: SocketTimeoutException) {
            log.warn("Connect timeout exception: " + e.message, e)
            listener.disconnected(this, e)
        } catch (e: ConnectException) {
            log.warn("Connect exception: " + e.message, e)
            listener.disconnected(this, e)
        } catch (e: IOException) {
            log.warn("IOException: " + e.message, e)
            listener.disconnected(this, e)
        } catch (e: InterruptedException) {
            log.warn("Peer connection thread interrupted.")
            listener.disconnected(this, null)
        } catch (e: Exception) {
            log.warn("Peer connection exception.", e)
            listener.disconnected(this, null)
        } finally {
            isRunning = false
        }
    }

    override fun close() {
        isRunning = false
        try {
            join(1000)
        } catch (e: InterruptedException) {
            log.error(e.message)
        }
    }

    override fun sendMessage(message: Message) {
        sendingQueue.add(message)
    }

    override fun setTimeout(timeoutInMillis: Long) {
        timeout = System.currentTimeMillis() + timeoutInMillis
    }
}
