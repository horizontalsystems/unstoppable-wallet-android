package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.HSCaution.Type
import io.horizontalsystems.bankwallet.core.ISendTronAdapter
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronAddressService
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronFeeService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tronkit.models.Contract
import io.horizontalsystems.tronkit.models.TransferContract
import io.horizontalsystems.tronkit.models.TriggerSmartContract
import io.horizontalsystems.tronkit.network.CreatedTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class SendTransactionServiceTron(token: Token) : AbstractSendTransactionService(false, false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Tron())
    private val adapter = App.adapterManager.getAdapterForToken<ISendTronAdapter>(token)!!
    private val nativeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native)) ?: throw IllegalArgumentException()

    private val amountService = SendAmountService(
        AmountValidator(),
        token.coin.code,
        adapter.balanceData.available.setScale(token.decimals, RoundingMode.DOWN),
        token.type.isNative,
    )
    private val addressService = SendTronAddressService(adapter, token)
    private val feeService = SendTronFeeService(adapter, nativeToken)

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeState = feeService.stateFlow.value

    private var networkFee: SendModule.AmountData? = null
    private var cautions: List<CautionViewItem> = listOf()
    private var sendTransactionData: SendTransactionData.Tron? = null
    private var loading = true
    private var nativeTokenAmount: BigDecimal? = null

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
            getAmountData(CoinValue(nativeToken, it))
        }

        cautions = buildList {
            val total = (nativeTokenAmount ?: BigDecimal.ZERO) + (feeState.fee ?: BigDecimal.ZERO)
            if (adapter.trxBalanceData.available < total) {
                add(
                    HSCaution(
                        s = TranslatableString.PlainString(Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalance_Title)),
                        type = Type.Error,
                        description = TranslatableString.PlainString(
                            Translator.getString(
                                R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
                                nativeToken.coin.code
                            )
                        )
                    ).toCautionViewItem()
                )
            }
        }

        emitState()
    }

    private suspend fun handleUpdatedAddressState(state: SendTronAddressService.State) {
        addressState = state

        feeService.setTronAddress(addressState.tronAddress)

        emitState()
    }

    private suspend fun handleUpdatedAmountState(state: SendAmountService.State) {
        amountState = state

        feeService.setAmount(amountState.amount)

        emitState()
    }

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        loading = false

        check(data is SendTransactionData.Tron)

        sendTransactionData = data

        if (data is SendTransactionData.Tron.WithContract) {
            feeService.setContract(data.contract)
            nativeTokenAmount = extractTrxSun(data.contract)?.toBigDecimal(nativeToken.decimals)
        } else if (data is SendTransactionData.Tron.WithCreateTransaction) {
            feeService.setCreatedTransaction(data.transaction)
            nativeTokenAmount = extractTrxSun(data.transaction).toBigDecimal(nativeToken.decimals)
        }

        emitState()
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        when (val tmpSendTransactionData = sendTransactionData) {
            is SendTransactionData.Tron.WithContract -> {
                adapter.send(tmpSendTransactionData.contract, feeState.feeLimit)
            }
            is SendTransactionData.Tron.WithCreateTransaction -> {
                adapter.send(tmpSendTransactionData.transaction)
            }
            null -> {
                throw IllegalStateException("Not supported")
            }
        }

        return SendTransactionResult.Tron
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = networkFee,
        cautions = cautions,
        sendable = feeState.canBeSend && cautions.none {
            it.type == CautionViewItem.Type.Error
        },
        loading = loading,
        fields = listOf(),
    )

    fun extractTrxSun(contract: Contract) = when (contract) {
        is TransferContract -> contract.amount
        is TriggerSmartContract -> contract.callValue
        else -> null
    }

    fun extractTrxSun(tx: CreatedTransaction): BigInteger {
        val rawData = tx.raw_data
        val contracts = rawData.contract

        var totalSun = BigInteger.ZERO

        for (contract in contracts) {
            val type = contract.type

            val parameter = contract.parameter
            val value = parameter.value

            when (type) {
                "TransferContract" -> {
                    value.amount?.let {
                        totalSun += it
                    }
                }

                "TriggerSmartContract",
                "CreateSmartContract" -> {
                    value.call_value?.let {
                        totalSun += it
                    }
                }
            }
        }

        return totalSun
    }
}
