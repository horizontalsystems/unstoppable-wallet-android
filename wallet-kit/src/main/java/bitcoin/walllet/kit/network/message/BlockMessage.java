package bitcoin.walllet.kit.network.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.struct.Block;

/**
 * Build P2P message:
 * https://en.bitcoin.it/wiki/Protocol_documentation#Message_structure
 *
 * @author liaoxuefeng
 */
public class BlockMessage extends Message {

    static final Logger log = LoggerFactory.getLogger(BlockMessage.class);

    public Block block;

    public BlockMessage() {
        super("block");
    }

    public BlockMessage(byte[] payload) throws IOException {
        super("block");
        try (BitcoinInput input = new BitcoinInput(new ByteArrayInputStream(payload))) {
            this.block = new Block(input);
        }
    }

    @Override
    protected byte[] getPayload() {
        return this.block.toByteArray();
    }

    /**
     * Validate block hash.
     */
    public boolean validateHash() {
        byte[] merkleHash = this.block.calculateMerkleHash();
        if (!Arrays.equals(merkleHash, this.block.header.merkleHash)) {
            log.error("Validate merckle hash failed.");
            return false;
        }
        // TODO: validate bits:
        return true;
    }

    @Override
    public String toString() {
        return "BlockMessage(txnCount=" + this.block.txns.length + ")";
    }

}
