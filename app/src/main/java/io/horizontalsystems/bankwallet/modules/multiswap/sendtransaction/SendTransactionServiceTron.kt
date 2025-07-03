package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendTronAdapter
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronAddressService
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronFeeService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.math.RoundingMode

class SendTransactionServiceTron(token: Token) : AbstractSendTransactionService() {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Tron())
    private val adapter = App.adapterManager.getAdapterForToken<ISendTronAdapter>(token)!!
    private val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native)) ?: throw IllegalArgumentException()

    val coinMaxAllowedDecimals = token.decimals
    val amountService = SendAmountService(
        AmountValidator(),
        token.coin.code,
        adapter.balanceData.available.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
        token.type.isNative,
    )
    val addressService = SendTronAddressService(adapter, token)
    val feeService = SendTronFeeService(adapter, feeToken)

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeState = feeService.stateFlow.value

    private var networkFee: SendModule.AmountData? = null
    private var sendTransactionData: SendTransactionData.Tron? = null

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
        coroutineScope.launch {
            feeService.stateFlow.collect {
                handleUpdatedFeeState(it)
            }
        }
    }

    private fun handleUpdatedFeeState(state: SendTronFeeService.State) {
        feeState = state

        networkFee = feeState.fee?.let {
            getAmountData(CoinValue(feeToken, it))
        }

        emitState()
    }

    private suspend fun handleUpdatedAddressState(state: SendTronAddressService.State) {
        addressState = state

        feeService.setAddress(addressState.address)

        emitState()
    }

    private suspend fun handleUpdatedAmountState(state: SendAmountService.State) {
        amountState = state

        feeService.setAmount(amountState.amount)

        emitState()
    }

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Tron)

        sendTransactionData = data

        if (data is SendTransactionData.Tron.WithContract) {
            feeService.setContract(data.contract)
        }

        emitState()

//        amountService.setAmount()

//        addressService.setAddress()
    }

    @Composable
    override fun GetSettingsContent(navController: NavController) {
//        TODO("Not yet implemented")
    }

    override suspend fun sendTransaction(): SendTransactionResult {
        when (val tmpSendTransactionData = sendTransactionData) {
            is SendTransactionData.Tron.WithContract -> {
                adapter.send(tmpSendTransactionData.contract)
            }
            is SendTransactionData.Tron.WithCreateTransaction -> {
                adapter.send(tmpSendTransactionData.transaction)
            }
            null -> {
                adapter.send(amountState.amount!!, addressState.tronAddress!!, feeState.feeLimit)
            }
        }

        return SendTransactionResult.Tron
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = networkFee,
        cautions = listOf(),
        sendable = sendTransactionData != null || (amountState.canBeSend && feeState.canBeSend && addressState.canBeSend),
        loading = false,
        fields = listOf(),
        extraFees = extraFees
    )
}
