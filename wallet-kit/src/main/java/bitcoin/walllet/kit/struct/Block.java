package bitcoin.walllet.kit.struct;

import java.io.IOException;
import java.util.Arrays;

import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.common.io.BitcoinOutput;
import bitcoin.walllet.kit.common.util.BytesUtils;
import bitcoin.walllet.kit.common.util.HashUtils;

/**
 * Bitcoin block data.
 *
 * @author Michael Liao
 */
public class Block {

    public Header header;
    public Transaction[] txns;

    private byte[] blockHash = null;

    public Block(BitcoinInput input) throws IOException {
        this.header = new Header(input);
        long txnCount = input.readVarInt(); // do not store txn_count
        this.txns = new Transaction[(int) txnCount];
        for (int i = 0; i < this.txns.length; i++) {
            this.txns[i] = new Transaction(input);
        }
    }

    public byte[] getBlockHash() {
        if (this.blockHash == null) {
            byte[] data = this.header.toByteArray();
            this.blockHash = HashUtils.doubleSha256(data);
        }
        return this.blockHash;
    }

    public byte[] calculateMerkleHash() {
        byte[][] hashes = Arrays.asList(this.txns).stream().map((tx) -> {
            return tx.getTxHash();
        }).toArray(byte[][]::new);
        while (hashes.length > 1) {
            hashes = merkleHash(hashes);
        }
        return hashes[0];
    }

    byte[][] merkleHash(byte[][] hashes) {
        int count = hashes.length / 2;
        int extra = hashes.length % 2;
        byte[][] results = new byte[count + extra][];
        for (int i = 0; i < count; i++) {
            results[i] = HashUtils.doubleSha256(BytesUtils.concat(hashes[2 * i], hashes[2 * i + 1]));
        }
        if (extra == 1) {
            results[count] = HashUtils
                    .doubleSha256(BytesUtils.concat(hashes[hashes.length - 1], hashes[hashes.length - 1]));
        }
        return results;
    }

    public byte[] toByteArray() {
        BitcoinOutput output = new BitcoinOutput();
        output.write(header.toByteArray());
        output.writeVarInt(this.txns.length);
        for (Transaction tx : txns) {
            output.write(tx.toByteArray());
        }
        return output.toByteArray();
    }
}
