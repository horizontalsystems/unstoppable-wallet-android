package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.ISendSolanaAdapter
import io.horizontalsystems.bankwallet.core.adapters.SolanaAdapter
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaAddressService
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

class SendTransactionServiceSolana(private val token: Token) : AbstractSendTransactionService(false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Tron())

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
    private val balance = App.solanaKitManager.solanaKitWrapper?.solanaKit?.balance ?: 0L
    private val solBalance = SolanaAdapter.balanceInBigDecimal(balance, solToken.decimals) - SolanaKit.accountRentAmount
    private val addressService = SendSolanaAddressService()

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    private var fee = SolanaKit.fee
    private var rawTransaction: ByteArray? = null

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
        }
//        amountService.setAmount(amount)
//        addressService.setAddress(address)
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val tmpRawTransaction = rawTransaction

        if (tmpRawTransaction != null) {
            adapter.send(tmpRawTransaction)
        } else {
            // todo checking amount should be in service
            val totalSolAmount = (if (token.type == TokenType.Native) amountState.amount!! else BigDecimal.ZERO) + SolanaKit.fee
            if (totalSolAmount > solBalance)
                throw EvmError.InsufficientBalanceWithFee

            adapter.send(amountState.amount!!, addressState.solanaAddress!!)
        }

        return SendTransactionResult.Solana
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = getAmountData(CoinValue(solToken, fee)),
        cautions = listOf(),
        sendable = rawTransaction != null || (amountState.canBeSend && addressState.canBeSend),
        loading = false,
        fields = listOf(),
        extraFees = extraFees
    )
}
