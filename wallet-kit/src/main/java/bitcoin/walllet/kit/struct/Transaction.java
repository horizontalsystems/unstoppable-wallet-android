package bitcoin.walllet.kit.struct;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.common.io.BitcoinOutput;
import bitcoin.walllet.kit.common.serializer.HashSerializer;
import bitcoin.walllet.kit.common.serializer.LockTimeSerializer;
import bitcoin.walllet.kit.common.util.HashUtils;

/**
 * Transaction.
 *
 * @author Michael Liao
 */
public class Transaction {

    /**
     * int32_t, transaction data format version (signed)
     */
    public int version;

    /**
     * a list of 1 or more transaction inputs or sources for coins
     */
    public TxIn[] tx_ins;

    /**
     * a list of 1 or more transaction outputs or destinations for coins
     */
    public TxOut[] tx_outs;

    /**
     * uint32_t, the block number or timestamp at which this transaction is
     * unlocked:
     * <p>
     * 0 Not locked
     * <p>
     * < 500000000 Block number at which this transaction is unlocked
     * <p>
     * >= 500000000 UNIX timestamp at which this transaction is unlocked
     * <p>
     * If all TxIn inputs have final (0xffffffff) sequence numbers then
     * lock_time is irrelevant. Otherwise, the transaction may not be added to a
     * block until after lock_time (see NLockTime).
     */
    @JsonSerialize(using = LockTimeSerializer.class)
    public long lock_time;

    private byte[] txHash = null;

    public Transaction(BitcoinInput input) throws IOException {
        this.version = input.readInt();
        long tx_in_count = input.readVarInt(); // do not store count
        this.tx_ins = new TxIn[(int) tx_in_count];
        for (int i = 0; i < this.tx_ins.length; i++) {
            this.tx_ins[i] = new TxIn(input);
        }
        long tx_out_count = input.readVarInt(); // do not store count
        this.tx_outs = new TxOut[(int) tx_out_count];
        for (int i = 0; i < this.tx_outs.length; i++) {
            this.tx_outs[i] = new TxOut(input);
        }
        this.lock_time = input.readUnsignedInt();
    }

    /**
     * Get transaction hash (actually calculate the hash of transaction data).
     */
    @JsonSerialize(using = HashSerializer.class)
    public byte[] getTxHash() {
        if (this.txHash == null) {
            this.txHash = HashUtils.doubleSha256(this.toByteArray());
        }
        return this.txHash;
    }

    public byte[] toByteArray() {
        BitcoinOutput buffer = new BitcoinOutput();
        buffer.writeInt(this.version).writeVarInt(this.tx_ins.length);
        for (int i = 0; i < this.tx_ins.length; i++) {
            buffer.write(tx_ins[i].toByteArray());
        }
        buffer.writeVarInt(this.tx_outs.length);
        for (int i = 0; i < this.tx_outs.length; i++) {
            buffer.write(tx_outs[i].toByteArray());
        }
        buffer.writeUnsignedInt(lock_time);
        return buffer.toByteArray();
    }

    public long getTxInCount() {
        return this.tx_ins.length;
    }

    public long getTxOutCount() {
        return this.tx_outs.length;
    }

    // http://bitcoin.stackexchange.com/questions/3374/how-to-redeem-a-basic-tx
    public void validate() {
        BitcoinOutput output = new BitcoinOutput();
        output.writeInt(this.version);
        // TODO:
    }

}
