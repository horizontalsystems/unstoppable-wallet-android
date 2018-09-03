package bitcoin.wallet.kit.messages

import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.walllet.kit.exceptions.BitcoinException
import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
import bitcoin.walllet.kit.utils.HashUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.logging.Logger

abstract class Message(cmd: String) {

    private var command: ByteArray

    init {
        command = getCommandBytes(cmd)
    }

    fun toByteArray(network: NetworkParameters): ByteArray {
        val payload = getPayload()
        return BitcoinOutput()
                .write(network.magicAsUInt32ByteArray())   // magic
                .write(command)                     // command: char[12]
                .writeInt(payload.size)             // length: uint32_t
                .write(getCheckSum(payload))        // checksum: uint32_t
                .write(payload)                     // payload:
                .toByteArray()
    }

    protected abstract fun getPayload(): ByteArray

    override fun toString(): String {
        return "Message(command=" + getCommandFrom(command) + ")"
    }

    object Builder {

        private val logger = Logger.getLogger("Builder")
        private val msgMap = initMessages()

        private fun initMessages(): Map<String, Class<*>> {
            val map = HashMap<String, Class<*>>()
            map["addr"] = AddrMessage::class.java
            map["block"] = BlockMessage::class.java
            map["getaddr"] = GetAddrMessage::class.java
            map["getblocks"] = GetBlocksMessage::class.java
            map["getdata"] = GetDataMessage::class.java
            map["getheaders"] = GetHeadersMessage::class.java
            map["inv"] = InvMessage::class.java
            map["ping"] = PingMessage::class.java
            map["pong"] = PongMessage::class.java
            map["verack"] = VerAckMessage::class.java
            map["version"] = VersionMessage::class.java
            map["headers"] = HeadersMessage::class.java
            map["merkleblock"] = MerkleBlockMessage::class.java
            map["tx"] = TransactionMessage::class.java
            map["filterload"] = FilterLoadMessage::class.java

            return map
        }

        /**
         * Parse stream as message.
         */
        @Throws(IOException::class)
        fun <T : Message> parseMessage(input: BitcoinInput, networkParameters: NetworkParameters): T {
            val magicAsBytesArray = input.readBytes(4)
            if (!Arrays.equals(magicAsBytesArray, networkParameters.magicAsUInt32ByteArray())) {
                throw BitcoinException("Bad magic.")
            }

            val command = getCommandFrom(input.readBytes(12))
            val payloadLength = input.readInt()
            val expectedChecksum = ByteArray(4)
            input.readFully(expectedChecksum)
            val payload = ByteArray(payloadLength)
            input.readFully(payload)

            // check:
            val actualChecksum = getCheckSum(payload)
            if (!Arrays.equals(expectedChecksum, actualChecksum)) {
                throw BitcoinException("Checksum failed.")
            }

            logger.info("MSG: $command payload ($payloadLength)")
            // build msg:
            val msgClass = msgMap[command] ?: return UnknownMessage(command, payload) as T
            try {
                val constructor = msgClass.getConstructor(ByteArray::class.java)
                return constructor.newInstance(payload) as T
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

        }
    }

    companion object {

        private fun getCommandFrom(cmd: ByteArray): String {
            var n = cmd.size - 1
            while (n >= 0) {
                if (cmd[n].toInt() == 0) {
                    n--
                } else {
                    break
                }
            }
            if (n <= 0) {
                throw BitcoinException("Bad command bytes.")
            }
            val b = Arrays.copyOfRange(cmd, 0, n + 1)
            return String(b, StandardCharsets.UTF_8)
        }

        private fun getCommandBytes(cmd: String): ByteArray {
            val cmdBytes = cmd.toByteArray()
            if (cmdBytes.isEmpty() || cmdBytes.size > 12) {
                throw IllegalArgumentException("Bad command: $cmd")
            }
            val buffer = ByteArray(12)
            System.arraycopy(cmdBytes, 0, buffer, 0, cmdBytes.size)
            return buffer
        }

        private fun getCheckSum(payload: ByteArray): ByteArray {
            val hash = HashUtils.doubleSha256(payload)
            return Arrays.copyOfRange(hash, 0, 4)
        }
    }
}
