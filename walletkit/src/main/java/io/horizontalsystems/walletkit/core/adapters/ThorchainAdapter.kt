package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.ISendThorchainAdapter
import io.horizontalsystems.walletkit.core.UnsupportedAccountException
import io.horizontalsystems.walletkit.core.managers.ThorchainKitWrapper
import io.horizontalsystems.walletkit.core.managers.toAdapterState
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.thorchainkit.models.Address
import io.horizontalsystems.thorchainkit.models.Denom
import io.horizontalsystems.thorchainkit.transaction.Signer
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ThorchainAdapter(
    thorchainKitWrapper: ThorchainKitWrapper,
    private val wallet: Wallet,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendThorchainAdapter {

    private val thorchainKit = thorchainKitWrapper.thorchainKit
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val denom = when (val tokenType = wallet.token.type) {
        is TokenType.Native -> Denom.RUNE
        is TokenType.ThorchainAsset -> tokenType.denom
        else -> throw IllegalStateException("Unsupported token type: $tokenType")
    }

    private val signer: Signer by lazy {
        when (val accountType = wallet.account.type) {
            is AccountType.Mnemonic -> Signer.getInstance(accountType.seed, thorchainKit.network)
            else -> throw UnsupportedAccountException()
        }
    }

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    override var balanceState: AdapterState = thorchainKit.syncState.toAdapterState()

    override val balanceData: BalanceData
        get() = BalanceData(availableBalance)

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val receiveAddress: String
        get() = thorchainKit.receiveAddress

    override val isMainNet = true

    override fun start() {
        coroutineScope.launch {
            thorchainKit.getDenomBalanceFlow(denom).collect {
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            thorchainKit.syncStateFlow.collect {
                balanceState = it.toAdapterState()
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            try {
                cachedFee = thorchainKit.estimateFee().toBigDecimal().movePointLeft(Denom.DECIMALS)
            } catch (_: Throwable) {
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override fun refresh() {
        thorchainKit.refresh()
    }

    override val debugInfo: String
        get() = ""

    // ISendThorchainAdapter

    override val availableBalance: BigDecimal
        get() = thorchainKit.getDenomBalance(denom).toBigDecimal().movePointLeft(Denom.DECIMALS)

    override val runeAvailableBalance: BigDecimal
        get() = thorchainKit.runeBalance.toBigDecimal().movePointLeft(Denom.DECIMALS)

    override val isNativeCoin = denom == Denom.RUNE

    // network fee is fixed; the cached value is refreshed from the node on start()
    private var cachedFee: BigDecimal = BigDecimal("0.02")

    override val fee: BigDecimal
        get() = cachedFee

    override fun validate(address: String) {
        Address.fromString(address, thorchainKit.network)
    }

    override suspend fun send(amount: BigDecimal, address: String, memo: String?): String {
        return thorchainKit.send(
            to = Address.fromString(address, thorchainKit.network),
            amount = amount.movePointRight(Denom.DECIMALS).toBigInteger(),
            denom = denom,
            memo = memo,
            signer = signer
        )
    }
}
