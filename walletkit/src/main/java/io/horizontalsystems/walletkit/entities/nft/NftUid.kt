package io.horizontalsystems.walletkit.entities.nft

// Only Solana transfers still produce NFT values; EVM NFT support was removed together with NftKit.
sealed class NftUid(val tokenId: String) {
    class Solana(mintAccountAddress: String) : NftUid(mintAccountAddress)

    val uid: String
        get() = when (this) {
            is Solana -> "solana|${tokenId}"
        }

    override fun equals(other: Any?): Boolean {
        return other is NftUid && other.uid == uid
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }
}
