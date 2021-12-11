package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger

class Eip20Adapter(
    context: Context,
    evmKit: EthereumKit,
    contractAddress: String,
    baseCoin: PlatformCoin,
    coinManager: ICoinManager,
    wallet: Wallet,
    private val fee: BigDecimal = BigDecimal.ZERO,
    override val minimumRequiredBalance: BigDecimal = BigDecimal.ZERO,
    override val minimumSendAmount: BigDecimal = BigDecimal.ZERO
) : BaseEvmAdapter(evmKit, wallet.decimal, coinManager) {

    private val transactionConverter = EvmTransactionConverter(coinManager, evmKit, wallet.transactionSource, baseCoin)

    private val contractAddress: Address = Address(contractAddress)
    val eip20Kit: Erc20Kit = Erc20Kit.getInstance(context, this.evmKit, this.contractAddress)

    val pendingTransactions: List<TransactionRecord>
        get() = eip20Kit.getPendingTransactions().map { transactionConverter.transactionRecord(it) }

    // IAdapter

    override fun start() {
        // started via EthereumKitManager
    }

    override fun stop() {
        // stopped via EthereumKitManager
    }

    override fun refresh() {
        eip20Kit.refresh()
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(eip20Kit.syncState)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = eip20Kit.syncStateFlowable.map { }

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(eip20Kit.balance, decimal))

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = eip20Kit.balanceFlowable.map { Unit }

    // ISendEthereumAdapter

    override fun sendInternal(address: Address, amount: BigInteger, gasPrice: Long, gasLimit: Long, logger: AppLogger): Single<Unit> {
        logger.info("call erc20Kit.buildTransferTransactionData")
        val transactionData = eip20Kit.buildTransferTransactionData(address, amount)

        return evmKit.send(transactionData, gasPrice, gasLimit)
                .doOnSubscribe {
                    logger.info("call ethereumKit.send")
                }
                .map {}
    }

    override fun estimateGasLimitInternal(toAddress: Address?, value: BigInteger, gasPrice: Long?): Single<Long> {
        if (toAddress == null) {
            return Single.just(evmKit.defaultGasLimit)
        }
        val transactionData = eip20Kit.buildTransferTransactionData(toAddress, value)

        return evmKit.estimateGas(transactionData, gasPrice)
    }

    override fun availableBalance(gasPrice: Long, gasLimit: Long): BigDecimal {
        return BigDecimal.ZERO.max(balanceData.available - fee)
    }

    override val ethereumBalance: BigDecimal
        get() = balanceInBigDecimal(evmKit.accountState?.balance, EvmAdapter.decimal)

    override fun getTransactionData(amount: BigInteger, address: Address): TransactionData {
        return eip20Kit.buildTransferTransactionData(address, amount)
    }

    private fun convertToAdapterState(syncState: SyncState): AdapterState = when (syncState) {
        is SyncState.Synced -> AdapterState.Synced
        is SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
        is SyncState.Syncing -> AdapterState.Syncing()
    }

    fun allowance(spenderAddress: Address, defaultBlockParameter: DefaultBlockParameter): Single<BigDecimal> {
        return eip20Kit.getAllowanceAsync(spenderAddress, defaultBlockParameter)
                .map {
                    scaleDown(it.toBigDecimal())
                }
    }

    companion object {
        fun clear(walletId: String, testMode: Boolean) {
            val networkTypes = when {
                testMode -> listOf(EthereumKit.NetworkType.EthRopsten)
                else -> listOf(EthereumKit.NetworkType.EthMainNet, EthereumKit.NetworkType.BscMainNet)
            }

            networkTypes.forEach {
                Erc20Kit.clear(App.instance, it, walletId)
            }
        }
    }

}
