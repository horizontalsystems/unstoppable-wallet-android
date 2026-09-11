package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import com.google.gson.Gson
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.adapters.Trc20Adapter
import io.horizontalsystems.walletkit.core.coinCodeWithNetwork
import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.core.HSCaution.Type
import io.horizontalsystems.walletkit.core.ISendTronAdapter
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.isNative
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.modules.amount.AmountValidator
import io.horizontalsystems.walletkit.modules.amount.SendAmountService
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.send.SendModule
import io.horizontalsystems.walletkit.modules.send.tron.SendTronAddressService
import io.horizontalsystems.walletkit.modules.send.tron.SendTronFeeService
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
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
import io.horizontalsystems.tronkit.models.Address as TronAddress

class SendTransactionServiceTron(private val token: Token) : AbstractSendTransactionService(false, false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Tron())
    private val gson = Gson()
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
    private var fields: List<DataField> = listOf()
    private var cautions: List<CautionViewItem> = listOf()
    private var sendTransactionData: SendTransactionData.Tron? = null
    private var loading = true
    private var nativeTokenAmount: BigDecimal? = null
    // A plain TRX send of the whole balance: the fee is taken out of the amount, as the
    // balance cannot cover both.
    private var sendMax = false
    private var adjustedAmount: BigDecimal? = null

    // Kit-side forms of the kit-free payloads in sendTransactionData, built in setSendTransactionData.
    private var builtContract: Contract? = null
    private var createdTransaction: CreatedTransaction? = null

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

        fields = listOfNotNull(
            feeState.resourcesConsumed?.let { DataFieldTronResources(it) },
            feeState.activationFee?.let { DataFieldTronActivationFee(getAmountData(CoinValue(nativeToken, it))) },
        )

        val fee = feeState.fee
        adjustedAmount = if (sendMax && fee != null) nativeTokenAmount?.minus(fee) else null
        val trxAmount = adjustedAmount ?: nativeTokenAmount ?: BigDecimal.ZERO

        cautions = buildList {
            val total = trxAmount + (fee ?: BigDecimal.ZERO)
            if (adapter.trxBalanceData.available < total) {
                add(
                    HSCaution(
                        s = TranslatableString.PlainString(Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalance_Title)),
                        type = Type.Error,
                        description = TranslatableString.PlainString(
                            Translator.getString(
                                R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
                                nativeToken.coinCodeWithNetwork
                            )
                        )
                    ).toCautionViewItem()
                )
            } else if (token.type.isNative && sendTransactionData is SendTransactionData.Tron.Simple && trxAmount <= BigDecimal.ZERO) {
                // The network rejects a zero-value TRX transfer; a max send whose fee eats
                // the whole balance ends here too.
                add(
                    HSCaution(
                        s = TranslatableString.PlainString(Translator.getString(R.string.Error)),
                        type = Type.Error,
                        description = TranslatableString.PlainString(
                            Translator.getString(R.string.Tron_ZeroAmountTrxNotAllowed, token.coin.code)
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

        when (data) {
            is SendTransactionData.Tron.Trc20Approve -> {
                val trc20Adapter = App.adapterManager.getAdapterForToken<Trc20Adapter>(token)
                checkNotNull(trc20Adapter)
                val contract = data.amount?.let {
                    trc20Adapter.approveTrc20TriggerSmartContract(data.spenderAddress, it)
                } ?: trc20Adapter.approveTrc20TriggerSmartContractUnlim(data.spenderAddress)
                builtContract = contract
                feeService.setContract(contract)
                nativeTokenAmount = extractTrxSun(contract)?.toBigDecimal(nativeToken.decimals)
            }
            is SendTransactionData.Tron.WithCreateTransaction -> {
                val transaction = gson.fromJson(data.rawTransaction, CreatedTransaction::class.java)
                createdTransaction = transaction
                feeService.setCreatedTransaction(transaction)
                nativeTokenAmount = extractTrxSun(transaction).toBigDecimal(nativeToken.decimals)
            }
            is SendTransactionData.Tron.Simple -> {
                nativeTokenAmount = if (token.type.isNative) data.amount else BigDecimal.ZERO
                sendMax = token.type.isNative && data.amount.compareTo(adapter.trxBalanceData.available) == 0
                feeService.setAmount(data.amount)
                feeService.setTronAddress(TronAddress.fromBase58(data.address))
            }
        }

        emitState()
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val txHash = when (val d = sendTransactionData) {
            is SendTransactionData.Tron.Trc20Approve -> {
                val contract = builtContract ?: throw IllegalStateException("Approve contract is not built")
                adapter.send(contract, feeState.feeLimit)
            }
            is SendTransactionData.Tron.WithCreateTransaction -> {
                val transaction = createdTransaction ?: throw IllegalStateException("Transaction is not parsed")
                adapter.send(transaction)
            }
            is SendTransactionData.Tron.Simple -> adapter.send(adjustedAmount ?: d.amount, TronAddress.fromBase58(d.address), feeState.feeLimit)
            null -> throw IllegalStateException("Not supported")
        }
        return SendTransactionResult.Tron(txHash = txHash)
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = networkFee,
        cautions = cautions,
        sendable = feeState.canBeSend && cautions.none {
            it.type == CautionViewItem.Type.Error
        },
        loading = loading,
        fields = fields,
        adjustedAmount = adjustedAmount,
    )

    // The total already includes bandwidth, energy and any activation fee.
    override val networkFeeInfoRes = R.string.FeeInfo_TronFee_Description

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
