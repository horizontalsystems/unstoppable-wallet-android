package io.horizontalsystems.walletkit.core

import io.horizontalsystems.walletkit.core.adapters.BitcoinFeeInfo
import io.horizontalsystems.walletkit.entities.TransactionDataSortMode
import io.horizontalsystems.walletkit.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import io.horizontalsystems.walletkit.core.providers.FeeRates
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.models.SignedRawTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

interface ISendBitcoinAdapter {
    val unspentOutputs: List<UnspentOutputInfo>
    val balanceData: BalanceData
    val blockchainType: BlockchainType
    fun selectUnspentOutputs(value: BigDecimal, feeRate: Int): List<UnspentOutputInfo>
    fun availableBalance(
        feeRate: Int,
        address: String?,
        memo: String?,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?,
        changeToFirstInput: Boolean,
        utxoFilters: UtxoFilters
    ): BigDecimal

    fun minimumSendAmount(address: String?): BigDecimal?
    fun bitcoinFeeInfo(
        amount: BigDecimal,
        feeRate: Int,
        address: String?,
        memo: String?,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?,
        changeToFirstInput: Boolean,
        filters: UtxoFilters
    ): BitcoinFeeInfo?

    fun validate(address: String, pluginData: Map<Byte, IPluginData>?)
    fun send(
        amount: BigDecimal,
        address: String,
        memo: String?,
        feeRate: Int,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?,
        transactionSorting: TransactionDataSortMode?,
        rbfEnabled: Boolean,
        changeToFirstInput: Boolean,
        utxoFilters: UtxoFilters
    ): BitcoinTransactionRecord?

    fun rawTransaction(
        amount: BigDecimal,
        address: String,
        memo: String?,
        feeRate: Int,
        unspentOutputs: List<UnspentOutputInfo>?,
        utxoFilters: UtxoFilters,
    ): SignedRawTransaction

    fun satoshiToBTC(value: Long): BigDecimal
}

interface IFeeRateProvider {
    val feeRateChangeable: Boolean get() = false
    suspend fun getFeeRates() : FeeRates
}
