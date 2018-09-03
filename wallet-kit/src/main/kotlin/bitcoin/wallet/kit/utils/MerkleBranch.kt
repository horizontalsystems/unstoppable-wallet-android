package bitcoin.wallet.kit.utils

import bitcoin.wallet.kit.exceptions.InvalidMerkleBlockException
import bitcoin.walllet.kit.hdwallet.Utils

class MerkleBranch(val txCount: Int, val hashes: Array<ByteArray>, val flags: ByteArray) {
    /** Bits used while traversing the tree  */
    private var bitsUsed: Int = 0

    /** Hashes used while traversing the tree  */
    private var hashesUsed: Int = 0

    @Throws(InvalidMerkleBlockException::class)
    fun calculateMerkleRoot(matchedHashes: MutableList<ByteArray>): ByteArray {
        matchedHashes.clear()
        bitsUsed = 0
        hashesUsed = 0
        //
        // Start at the root and travel down to the leaf node
        //
        var height = 0
        while (getTreeWidth(height) > 1)
            height++
        val merkleRoot = parseBranch(height, 0, matchedHashes)
        //
        // Verify that all bits and hashes were consumed
        //
        if ((bitsUsed + 7) / 8 != flags.size)
            throw InvalidMerkleBlockException("Merkle branch did not use all of its bits")
        if (hashesUsed != hashes.size)
            throw InvalidMerkleBlockException(String.format("Merkle branch used %d of %d hashes",
                    hashesUsed, hashes.size))
        return merkleRoot
    }

    @Throws(InvalidMerkleBlockException::class)
    private fun parseBranch(height: Int, pos: Int, matchedHashes: MutableList<ByteArray>): ByteArray {
        if (bitsUsed >= flags.size * 8)
            throw InvalidMerkleBlockException("Merkle branch overflowed the bits array")
        val parentOfMatch = Utils.checkBitLE(flags, bitsUsed++)
        if (height == 0 || !parentOfMatch) {
            //
            // If at height 0 or nothing interesting below, use the stored hash and do not descend
            // to the next level.  If we have a match at height 0, it is a matching transaction.
            //
            if (hashesUsed >= hashes.size)
                throw InvalidMerkleBlockException("Merkle branch overflowed the hash array")
            if (height == 0 && parentOfMatch)
                matchedHashes.add(hashes[hashesUsed])
            return hashes[hashesUsed++]
        }
        //
        // Continue down to the next level
        //
        val right: ByteArray
        val left = parseBranch(height - 1, pos * 2, matchedHashes)
        if (pos * 2 + 1 < getTreeWidth(height - 1))
            right = parseBranch(height - 1, pos * 2 + 1, matchedHashes)
        else
            right = left

        return Utils.doubleDigestTwoBuffers(left, 0, 32, right, 0, 32)
    }

    private fun getTreeWidth(height: Int): Int {
        return txCount + (1 shl height) - 1 shr height
    }

}