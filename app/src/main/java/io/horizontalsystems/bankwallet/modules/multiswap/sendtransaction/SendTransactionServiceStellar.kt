package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendStellarAdapter
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.stellar.SendStellarAddressService
import io.horizontalsystems.bankwallet.modules.send.stellar.SendStellarMinimumAmountService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SendTransactionServiceStellar(token: Token) : AbstractSendTransactionService() {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Tron())

    private val adapter = App.adapterManager.getAdapterForToken<ISendStellarAdapter>(token)!!
    private val fee = adapter.fee

    private val amountService = SendAmountService(
        amountValidator = AmountValidator(),
        coinCode = token.coin.code,
        availableBalance = adapter.maxSendableBalance,
        leaveSomeBalanceForFee = token.type.isNative
    )
    private val addressService = SendStellarAddressService()
    private val minimumAmountService = SendStellarMinimumAmountService(adapter)
    private val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Stellar, TokenType.Native)) ?: throw IllegalArgumentException()

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var minimumAmountState = minimumAmountService.stateFlow.value
    private var memo: String? = null
    private var transactionEnvelope: String? = null

    private var networkFee: SendModule.AmountData? = getAmountData(CoinValue(feeToken, fee))

    override fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Default) {
            amountService.stateFlow.collect {
                handleUpdatedAmountState(it)
            }
        }
        coroutineScope.launch(Dispatchers.Default) {
            addressService.stateFlow.collect {
                handleUpdatedAddressState(it)
            }
        }
        coroutineScope.launch(Dispatchers.Default) {
            minimumAmountService.stateFlow.collect {
                handleUpdatedMinimumAmountState(it)
            }
        }
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private suspend fun handleUpdatedAddressState(addressState: SendStellarAddressService.State) {
        this.addressState = addressState

        minimumAmountService.setValidAddress(addressState.validAddress)

        emitState()
    }

    private fun handleUpdatedMinimumAmountState(state: SendStellarMinimumAmountService.State) {
        minimumAmountState = state

        amountService.setMinimumSendAmount(minimumAmountState.minimumAmount)

        emitState()
    }

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Stellar)

        when (data) {
            is SendTransactionData.Stellar.Regular -> {
                memo = data.memo
                addressService.setAddress(Address(data.address))
                amountService.setAmount(data.amount)
            }
            is SendTransactionData.Stellar.WithTransactionEnvelope -> {
                transactionEnvelope = data.transactionEnvelope
                emitState()
            }
        }
    }

    @Composable
    override fun GetSettingsContent(navController: NavController) {
        TODO("Not yet implemented")
    }

    override suspend fun sendTransaction(): SendTransactionResult {
        val transactionEnvelope = transactionEnvelope
        if (transactionEnvelope != null) {
            adapter.send(transactionEnvelope)
        } else {
            adapter.send(amountState.amount!!, addressState.address?.hex!!, memo)
        }

        return SendTransactionResult.Stellar
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = networkFee,
        cautions = listOf(),
        sendable = transactionEnvelope != null || (amountState.canBeSend && addressState.canBeSend && minimumAmountState.canBeSend),
        loading = false,
        fields = listOf(),
        extraFees = extraFees
    )
}
