package bitcoin.walllet.kit.network.message;

/**
 * ACK for Version Message.
 *
 * @author liaoxuefeng
 */
public class VerAckMessage extends Message {

    public VerAckMessage() {
        super("verack");
    }

    public VerAckMessage(byte[] payload) {
        super("verack");
    }

    @Override
    protected byte[] getPayload() {
        return new byte[0];
    }

    @Override
    public String toString() {
        return "VerAckMessage()";
    }

}
