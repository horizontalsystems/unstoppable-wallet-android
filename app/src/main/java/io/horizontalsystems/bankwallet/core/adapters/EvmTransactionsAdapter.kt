package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.models.TransactionTag
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger

class EvmTransactionsAdapter(kit: EthereumKit, baseCoin: PlatformCoin, coinManager: ICoinManager, source: TransactionSource) :
    BaseEvmAdapter(kit, EvmAdapter.decimal, coinManager), ITransactionsAdapter {

    // IAdapter

    private val transactionConverter = EvmTransactionConverter(coinManager, evmKit, source, baseCoin)

    override fun start() {
        // started via EthereumKitManager
    }

    override fun stop() {
        // stopped via EthereumKitManager
    }

    override fun refresh() {
        // refreshed via EthereumKitManager
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(evmKit.syncState)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = evmKit.syncStateFlowable.map {}

    override fun sendInternal(
        address: Address,
        amount: BigInteger,
        gasPrice: Long,
        gasLimit: Long,
        logger: AppLogger
    ): Single<Unit> {
        return evmKit.send(address, amount, byteArrayOf(), gasPrice, gasLimit)
            .doOnSubscribe {
                logger.info("call ethereumKit.send")
            }
            .map { }
    }

    override fun estimateGasLimitInternal(
        toAddress: Address?,
        value: BigInteger,
        gasPrice: Long?
    ): Single<Long> {
        return evmKit.estimateGas(toAddress, value, gasPrice)
    }

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(evmKit.accountState?.balance, decimal))

    override val minimumRequiredBalance: BigDecimal
        get() = BigDecimal.ZERO

    override val minimumSendAmount: BigDecimal
        get() = BigDecimal.ZERO

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = evmKit.accountStateFlowable.map { }

    // ITransactionsAdapter

    override val transactionsState: AdapterState
        get() = convertToAdapterState(evmKit.transactionsSyncState)

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = evmKit.transactionsSyncStateFlowable.map {}

    override val explorerTitle: String
        get() = when (evmKit.networkType) {
            EthereumKit.NetworkType.EthMainNet,
            EthereumKit.NetworkType.EthRopsten,
            EthereumKit.NetworkType.EthKovan,
            EthereumKit.NetworkType.EthRinkeby,
            EthereumKit.NetworkType.EthGoerli -> "etherscan.io"
            EthereumKit.NetworkType.BscMainNet -> "bscscan.com"
        }

    override fun explorerUrl(transactionHash: String): String? {
        val domain = when (evmKit.networkType) {
            EthereumKit.NetworkType.EthMainNet -> "etherscan.io"
            EthereumKit.NetworkType.EthRopsten -> "ropsten.etherscan.io"
            EthereumKit.NetworkType.EthKovan -> "kovan.etherscan.io"
            EthereumKit.NetworkType.EthRinkeby -> "rinkeby.etherscan.io"
            EthereumKit.NetworkType.BscMainNet -> "bscscan.com"
            EthereumKit.NetworkType.EthGoerli -> "goerli.etherscan.io"
        }

        return "https://$domain/tx/$transactionHash"
    }

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        coin: PlatformCoin?,
        limit: Int,
        transactionType: FilterTransactionType
    ): Single<List<TransactionRecord>> {
        return evmKit.getTransactionsAsync(
            getFilters(coin, transactionType),
            from?.transactionHash?.hexStringToByteArray(),
            limit
        ).map {
            it.map { tx -> transactionConverter.transactionRecord(tx) }
        }
    }

    override fun getTransactionRecordsFlowable(coin: PlatformCoin?, transactionType: FilterTransactionType): Flowable<List<TransactionRecord>> {
        return evmKit.getTransactionsFlowable(getFilters(coin, transactionType)).map {
            it.map { tx -> transactionConverter.transactionRecord(tx) }
        }
    }

    private fun convertToAdapterState(syncState: EthereumKit.SyncState): AdapterState =
        when (syncState) {
            is EthereumKit.SyncState.Synced -> AdapterState.Synced
            is EthereumKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is EthereumKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    // ISendEthereumAdapter

    override val ethereumBalance: BigDecimal
        get() = balanceData.available

    override fun availableBalance(gasPrice: Long, gasLimit: Long): BigDecimal {
        return BigDecimal.ZERO.max(balanceData.available - fee(gasPrice, gasLimit))
    }

    override fun getTransactionData(amount: BigInteger, address: Address): TransactionData {
        return TransactionData(address, amount, byteArrayOf())
    }

    private fun coinTagName(coin: PlatformCoin) = when (val type = coin.coinType) {
        CoinType.Ethereum, CoinType.BinanceSmartChain -> TransactionTag.EVM_COIN
        is CoinType.Erc20 -> type.address
        is CoinType.Bep20 -> type.address
        else -> throw IllegalArgumentException()
    }

    private fun getFilters(coin: PlatformCoin?, filter: FilterTransactionType): List<List<String>> {
        val filterCoin = coin?.let {
            coinTagName(it)
        }

        val filterTag = when (filter) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> when {
                coin != null -> TransactionTag.eip20Incoming(coinTagName(coin))
                else -> TransactionTag.INCOMING
            }
            FilterTransactionType.Outgoing -> when {
                coin != null -> TransactionTag.eip20Outgoing(coinTagName(coin))
                else -> TransactionTag.OUTGOING
            }
            FilterTransactionType.Swap -> TransactionTag.SWAP
            FilterTransactionType.Approve -> TransactionTag.EIP20_APPROVE
        }

        return listOfNotNull(filterCoin, filterTag).map { listOf(it) }
    }

    companion object {
        const val decimal = 18

        fun clear(walletId: String, testMode: Boolean) {
            val networkTypes = when {
                testMode -> listOf(EthereumKit.NetworkType.EthRopsten)
                else -> listOf(
                    EthereumKit.NetworkType.EthMainNet,
                    EthereumKit.NetworkType.BscMainNet
                )
            }
            networkTypes.forEach {
                EthereumKit.clear(App.instance, it, walletId)
            }
        }
    }
}
