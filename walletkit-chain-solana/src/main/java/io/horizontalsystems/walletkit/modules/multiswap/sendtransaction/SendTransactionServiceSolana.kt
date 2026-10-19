package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.walletkit.chain.solana.SolanaChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.EvmError
import io.horizontalsystems.walletkit.core.ISendSolanaAdapter
import io.horizontalsystems.walletkit.core.adapters.SolanaAdapter
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.isNative
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.modules.amount.AmountValidator
import io.horizontalsystems.walletkit.modules.amount.SendAmountService
import io.horizontalsystems.walletkit.modules.send.solana.SendSolanaAddressService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.solanakit.SolanaKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class SendTransactionServiceSolana(private val token: Token) : AbstractSendTransactionService(false, false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Solana())

    private val adapter = App.adapterManager.getAdapterForToken<ISendSolanaAdapter>(token)!!

    private val amountValidator = AmountValidator()
    private val coinMaxAllowedDecimals = token.decimals

    private val amountService = SendAmountService(
        amountValidator,
        token.coin.code,
        adapter.availableBalance.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
        token.type.isNative,
    )
    private val solToken = App.coinManager.getToken(TokenQuery(BlockchainType.Solana, TokenType.Native)) ?: throw IllegalArgumentException()
    private val balance = (ChainRegistry[BlockchainType.Solana] as SolanaChainPlugin).kitManager.solanaKitWrapper?.solanaKit?.balance ?: 0L
    private val solBalance = SolanaAdapter.balanceInBigDecimal(balance, solToken.decimals) - SolanaKit.accountRentAmount
    private val addressService = SendSolanaAddressService()

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    private var fee = SolanaKit.fee
    private var rawTransaction: ByteArray? = null
    private var simpleDataSet = false

    override fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            amountService.stateFlow.collect {
                handleUpdatedAmountState(it)
            }
        }

        coroutineScope.launch {
            addressService.stateFlow.collect {
                handleUpdatedAddressState(it)
            }
        }
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendSolanaAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Solana)

        when (data) {
            is SendTransactionData.Solana.WithRawTransaction -> {
                this.rawTransaction = data.rawTransaction
                fee = adapter.estimateFee(data.rawTransaction)
                emitState()
            }
            is SendTransactionData.Solana.Simple -> {
                amountService.setAmount(data.amount)
                addressService.setAddress(io.horizontalsystems.walletkit.entities.Address(data.address))
                simpleDataSet = true
                emitState()
            }
        }
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        checkBroadcastAllowed()
        val tmpRawTransaction = rawTransaction
        val fullTransaction: io.horizontalsystems.solanakit.models.FullTransaction

        if (tmpRawTransaction != null) {
            fullTransaction = adapter.send(tmpRawTransaction)
        } else {
            val totalSolAmount = (if (token.type == TokenType.Native) amountState.amount!! else BigDecimal.ZERO) + SolanaKit.fee
            if (totalSolAmount > solBalance)
                throw EvmError.InsufficientBalanceWithFee
            fullTransaction = adapter.send(amountState.amount!!, addressState.solanaAddress!!)
        }

        return SendTransactionResult.Solana(txHash = fullTransaction.transaction.hash)
    }

    override fun maxSendableAmount(): BigDecimal? =
        if (token.type == TokenType.Native) (solBalance - fee).max(BigDecimal.ZERO) else null

    // The amount and address services are collected asynchronously, so this instance keeps
    // describing the previous transfer until they run; a replaced deposit gets a fresh instance.
    override fun fresh(): AbstractSendTransactionService = SendTransactionServiceSolana(token)

    // A plain transfer is loading until both collectors have reported on the data that was set:
    // an amount, and either a parsed address or the reason it failed to parse.
    private val simpleTransferSettled: Boolean
        get() = simpleDataSet && amountState.amount != null &&
                (addressState.solanaAddress != null || addressState.addressError != null)

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = getAmountData(CoinValue(solToken, fee)),
        cautions = listOfNotNull(
            amountState.amountCaution?.toCautionViewItem(),
            addressState.addressError?.let { CautionViewItem.fromThrowable(it) },
        ),
        sendable = rawTransaction != null || (amountState.canBeSend && addressState.canBeSend),
        loading = rawTransaction == null && !simpleTransferSettled,
        fields = listOf(),
    )
}
