package bitcoin.walllet.kit.network.message;

/**
 * Build P2P message:
 * https://en.bitcoin.it/wiki/Protocol_documentation#Message_structure
 *
 * @author liaoxuefeng
 */
public class UnknownMessage extends Message {

    private byte[] payload;

    public UnknownMessage(String command, byte[] payload) {
        super(command);
    }

    @Override
    protected byte[] getPayload() {
        return this.payload;
    }

}
