package io.horizontalsystems.bankwallet.core.adapters.nft

import io.horizontalsystems.bankwallet.entities.nft.EvmNftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nftkit.core.NftKit
import io.horizontalsystems.nftkit.models.NftBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class EvmNftAdapter(
    private val blockchainType: BlockchainType,
    private val nftKit: NftKit,
    address: Address
) : INftAdapter {

    override val userAddress = address.hex

    override val nftRecordsFlow: Flow<List<NftRecord>>
        get() = nftKit.nftBalancesFlow.map { nftBalances -> nftBalances.map { record(it) } }

    override val nftRecords: List<NftRecord>
        get() = nftKit.nftBalances.map { record(it) }

    override fun sync() {
        nftKit.sync()
    }

    override fun nftRecord(nftUid: NftUid): NftRecord? {
        val evm = (nftUid as? NftUid.Evm) ?: return null

        val tokenId = evm.tokenId.toBigIntegerOrNull() ?: return null

        val contractAddress = Address(evm.contractAddress)

        val nftBalance = nftKit.nftBalance(contractAddress, tokenId) ?: return null

        return record(nftBalance)
    }

    override fun transferEip721TransactionData(
        contractAddress: String,
        to: Address,
        tokenId: String
    ): TransactionData? {
        val address = Address(contractAddress)
        val tokenIdBigInt = tokenId.toBigIntegerOrNull() ?: return null
        return nftKit.transferEip721TransactionData(address, to, tokenIdBigInt)
    }

    override fun transferEip1155TransactionData(
        contractAddress: String,
        to: Address,
        tokenId: String,
        value: BigInteger,
    ): TransactionData? {
        val address = Address(contractAddress)
        val tokenIdBigInt = tokenId.toBigIntegerOrNull() ?: return null
        return nftKit.transferEip1155TransactionData(address, to, tokenIdBigInt, value)
    }

    private fun record(nftBalance: NftBalance): EvmNftRecord {
        return EvmNftRecord(
            blockchainType = blockchainType,
            nftType = nftBalance.nft.type,
            contractAddress = nftBalance.nft.contractAddress.hex,
            tokenId = nftBalance.nft.tokenId.toString(),
            tokenName = nftBalance.nft.tokenName,
            balance = nftBalance.balance
        )
    }
}