package io.horizontalsystems.bankwallet.core.adapters.nft

import io.horizontalsystems.bankwallet.entities.nft.NftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.entities.nft.SolanaNftRecord
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.models.FullTokenAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SolanaNftAdapter(
        private val blockchainType: BlockchainType,
        private val solanaKit: SolanaKit,
) : INftAdapter {

    override val userAddress = solanaKit.receiveAddress

    override val nftRecordsFlow: Flow<List<NftRecord>>
        get() = solanaKit.nonFungibleTokenAccountsFlow.map { nftAccounts -> nftAccounts.map { record(it) } }

    override val nftRecords: List<NftRecord>
        get() = solanaKit.nonFungibleTokenAccounts().map { record(it) }

    override fun sync() {
        // This is handled by SolanaKitManager
    }

    override fun nftRecord(nftUid: NftUid): NftRecord? {
        val solanaNft = (nftUid as? NftUid.Solana) ?: return null
        val nftBalance = solanaKit.tokenAccount(solanaNft.tokenId) ?: return null

        return record(nftBalance)
    }

    private fun record(fullTokenAccount: FullTokenAccount): SolanaNftRecord {
        return SolanaNftRecord(
                blockchainType = blockchainType,
                collectionAddress = fullTokenAccount.mintAccount.collectionAddress,
                tokenId = fullTokenAccount.mintAccount.address,
                tokenName = fullTokenAccount.mintAccount.name,
                balance = fullTokenAccount.tokenAccount.balance.toInt()
        )
    }
}
