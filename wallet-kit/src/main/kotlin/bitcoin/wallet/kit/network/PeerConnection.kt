package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.messages.Message
import bitcoin.wallet.kit.messages.VersionMessage
import bitcoin.walllet.kit.io.BitcoinInput
import java.io.IOException
import java.lang.Exception
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class PeerConnection(val host: String, private val network: NetworkParameters, private val listener: Listener) : Thread() {

    interface Listener {
        fun onMessage(message: Message)
        fun disconnected(e: Exception? = null)
    }

    private val logger = Logger.getLogger("PeerConnection")
    private val sendingQueue: BlockingQueue<Message> = ArrayBlockingQueue(100)
    private val socket = Socket()

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
            socket.connect(InetSocketAddress(host, network.port), 10000)
            socket.soTimeout = 10000

            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            logger.info("Socket $host connected.")
            setTimeout(60000)

            // add version message to send automatically:
            sendMessage(VersionMessage(0, socket.inetAddress, network))
            // loop:
            while (isRunning) {
                if (isTimeout) {
                    logger.info("Timeout!")
                    break
                }

                // try get message to send:
                val msg = sendingQueue.poll(1, TimeUnit.SECONDS)
                if (isRunning && msg != null) {
                    // send message:
                    logger.info("=> " + msg.toString())
                    output.write(msg.toByteArray(network))
                }

                // try receive message:
                if (isRunning && input.available() > 0) {
                    val inputStream = BitcoinInput(input)
                    val parsedMsg = Message.Builder.parseMessage<Message>(inputStream, network)
                    logger.info("<= $parsedMsg")
                    listener.onMessage(parsedMsg)
                }
            }

            listener.disconnected()
        } catch (e: SocketTimeoutException) {
            logger.log(Level.SEVERE, "Connect timeout exception: ${e.message}", e)
            listener.disconnected(e)
        } catch (e: ConnectException) {
            logger.log(Level.SEVERE, "Connect exception: ${e.message}", e)
            listener.disconnected(e)
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "IOException: ${e.message}", e)
            listener.disconnected(e)
        } catch (e: InterruptedException) {
            logger.log(Level.SEVERE, "Peer connection thread interrupted.")
            listener.disconnected()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Peer connection exception.", e)
            listener.disconnected()
        } finally {
            isRunning = false
        }
    }

    fun close() {
        isRunning = false
        try {
            join(1000)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, e.message)
        }
    }

    fun sendMessage(message: Message) {
        sendingQueue.add(message)
    }

    fun setTimeout(timeoutInMillis: Long) {
        timeout = System.currentTimeMillis() + timeoutInMillis
    }

}
