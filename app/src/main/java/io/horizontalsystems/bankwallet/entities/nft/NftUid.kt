package io.horizontalsystems.bankwallet.entities.nft

import io.horizontalsystems.marketkit.models.BlockchainType

sealed class NftUid(val tokenId: String) {
    class Evm(val blockchainType: BlockchainType, val contractAddress: String, tokenId: String) : NftUid(tokenId)
    class Solana(val contactAddress: String, tokenId: String) : NftUid(tokenId)

    val uid: String
        get() = when (this) {
            is Evm -> "evm|${blockchainType.uid}|${contractAddress}|${tokenId}"
            is Solana -> "solana|${contactAddress}|${tokenId}"
        }

    override fun equals(other: Any?): Boolean {
        return other is NftUid && other.uid == uid
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    companion object {
        fun fromUid(uid: String): NftUid {
            val parts = uid.split("|")

            return try {
                when (parts[0]) {
                    "evm" -> {
                        Evm(BlockchainType.fromUid(parts[1]), parts[2], parts[3])
                    }
                    "sol" -> {
                        Solana(parts[1], parts[2])
                    }
                    else -> {
                        throw IllegalStateException()
                    }
                }
            } catch (error: Throwable) {
                throw IllegalStateException("Could not parse NftUid: $uid")
            }
        }
    }
}