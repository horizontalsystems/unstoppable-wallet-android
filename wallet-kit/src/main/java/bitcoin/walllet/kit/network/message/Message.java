package bitcoin.walllet.kit.network.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import bitcoin.wallet.kit.message.HeadersMessage;
import bitcoin.wallet.kit.message.MerkleBlockMessage;
import bitcoin.wallet.kit.message.TransactionMessage;
import bitcoin.walllet.kit.common.exceptions.BitcoinException;
import bitcoin.walllet.kit.common.constant.BitcoinConstants;
import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.common.io.BitcoinOutput;
import bitcoin.walllet.kit.common.util.HashUtils;


/**
 * P2P message:
 * https://en.bitcoin.it/wiki/Protocol_documentation#Message_structure
 *
 * @author liaoxuefeng
 */
public abstract class Message {

    byte[] command;

    public Message(String cmd) {
        this.command = getCommandBytes(cmd);
    }

    static String getCommandFrom(byte[] cmd) {
        int n = cmd.length - 1;
        while (n >= 0) {
            if (cmd[n] == 0) {
                n--;
            } else {
                break;
            }
        }
        if (n <= 0) {
            throw new BitcoinException("Bad command bytes.");
        }
        byte[] b = Arrays.copyOfRange(cmd, 0, n + 1);
        return new String(b, StandardCharsets.UTF_8);
    }

    static byte[] getCommandBytes(String cmd) {
        byte[] cmdBytes = cmd.getBytes();
        if (cmdBytes.length < 1 || cmdBytes.length > 12) {
            throw new IllegalArgumentException("Bad command: " + cmd);
        }
        byte[] buffer = new byte[12];
        System.arraycopy(cmdBytes, 0, buffer, 0, cmdBytes.length);
        return buffer;
    }

    static byte[] getCheckSum(byte[] payload) {
        byte[] hash = HashUtils.doubleSha256(payload);
        return Arrays.copyOfRange(hash, 0, 4);
    }

    public byte[] toByteArray() {
        byte[] payload = getPayload();
        return new BitcoinOutput()
                .writeInt(BitcoinConstants.MAGIC)   // magic
                .write(this.command)                // command: char[12]
                .writeInt(payload.length)           // length: uint32_t
                .write(getCheckSum(payload))        // checksum: uint32_t
                .write(payload)                     // payload:
                .toByteArray();
    }

    protected abstract byte[] getPayload();

    @Override
    public String toString() {
        return "Message(command=" + getCommandFrom(this.command) + ")";
    }

    public static class Builder {

        // static final Logger log = Logger.getLogger( Builder.class.getName());
        static final Logger log = LoggerFactory.getLogger(Builder.class);
        static final Map<String, Class<?>> msgMap = initMessages();

        private static Map<String, Class<?>> initMessages() {
            Map<String, Class<?>> map = new HashMap<>();
            map.put("addr", AddrMessage.class);
            map.put("block", BlockMessage.class);
            map.put("getaddr", GetAddrMessage.class);
            map.put("getblocks", GetBlocksMessage.class);
            map.put("getdata", GetDataMessage.class);
            map.put("getheaders", GetHeadersMessage.class);
            map.put("inv", InvMessage.class);
            map.put("ping", PingMessage.class);
            map.put("pong", PongMessage.class);
            map.put("verack", VerAckMessage.class);
            map.put("version", VersionMessage.class);
            map.put("headers", HeadersMessage.class);
            map.put("merkleblock", MerkleBlockMessage.class);
            map.put("tx", TransactionMessage.class);
            return map;
        }

        /**
         * Parse stream as message.
         */
        @SuppressWarnings("unchecked")
        public static <T extends Message> T parseMessage(BitcoinInput input) throws IOException {
            if (input.readInt() != BitcoinConstants.MAGIC) {
                throw new BitcoinException("Bad magic.");
            }

            String command = getCommandFrom(input.readBytes(12));
            int payloadLength = input.readInt();
            byte[] expectedChecksum = new byte[4];
            input.readFully(expectedChecksum);
            byte[] payload = new byte[payloadLength];
            input.readFully(payload);
            // check:
            byte[] actualChecksum = getCheckSum(payload);
            if (!Arrays.equals(expectedChecksum, actualChecksum)) {
                throw new BitcoinException("Checksum failed.");
            }
            log.info("MSG: " + command + " payload (" + payloadLength + ")");
            // build msg:
            Class<?> msgClass = msgMap.get(command);
            if (msgClass == null) {
                return (T) new UnknownMessage(command, payload);
            }
            try {
                Constructor<?> constructor = msgClass.getConstructor(byte[].class);
                return (T) constructor.newInstance(payload);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
