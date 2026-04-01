package cash.p.terminal.modules.multiswap.sendtransaction.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendTonAdapter
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.getFeeTokenBalance
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.isNative
import cash.p.terminal.core.managers.PendingTransactionRegistrar
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.entities.PendingTransactionDraft
import cash.p.terminal.modules.address.AddressHandlerTon
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.multiswap.sendtransaction.ISendTransactionService
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionServiceState
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.send.ton.FeeStatus
import cash.p.terminal.modules.send.ton.SendTonAddressService
import cash.p.terminal.modules.send.ton.SendTonAmountService
import cash.p.terminal.modules.send.ton.SendTonFeeService
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.getMaxSendableBalance
import io.horizontalsystems.core.entities.CurrencyValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal
import kotlin.getValue

class SendTransactionServiceTon(
    token: Token
) : ISendTransactionService<ISendTonAdapter>(token) {
    private val amountValidator = AmountValidator()

    private val adjustedAvailableBalance: BigDecimal
        get() = adapterManager.getMaxSendableBalance(wallet, adapter.maxSpendableBalance)

    private val amountService = SendTonAmountService(
        amountValidator = amountValidator,
        coinCode = wallet.coin.code,
        availableBalance = adjustedAvailableBalance,
        leaveSomeBalanceForFee = wallet.token.type.isNative
    )
    private val addressService = SendTonAddressService()
    private val addressHandlerTon = AddressHandlerTon()
    private val feeService = SendTonFeeService(adapter)
    private val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = AppConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeState = feeService.stateFlow.value

    private val pendingRegistrar: PendingTransactionRegistrar by inject(PendingTransactionRegistrar::class.java)
    private var pendingTxId: String? = null

    var coinRate by mutableStateOf(xRateService.getRate(token.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set

    private val accountManager: IAccountManager by inject(IAccountManager::class.java)
    private var feeCaution: CautionViewItem? = null
    private var hasEnoughFeeAmount: Boolean = true

    override fun createState() = SendTransactionServiceState(
        availableBalance = adjustedAvailableBalance,
        networkFee = feeAmountData,
        cautions = cautions,
        feeCaution = feeCaution,
        sendable = sendable,
        loading = loading,
        fields = fields
    )

    private fun handleUpdatedAmountState(amountState: SendTonAmountService.State) {
        this.amountState = amountState

        feeService.setAmount(amountState.amount)
        feeService.setMemo(amountState.memo)

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendTonAddressService.State) {
        this.addressState = addressState

        feeService.setTonAddress(addressState.tonAddress)

        emitState()
    }

    private fun handleUpdatedFeeState(feeState: SendTonFeeService.State) {
        this.feeState = feeState
        checkFeeBalance(feeState)

        if (feeState.feeStatus is FeeStatus.Success) {
            val primaryAmountInfo =
                SendModule.AmountInfo.CoinValueInfo(CoinValue(feeToken, feeState.feeStatus.fee))
            val secondaryAmountInfo = rate?.let {
                SendModule.AmountInfo.CurrencyValueInfo(
                    CurrencyValue(
                        it.currency,
                        it.value * feeState.feeStatus.fee
                    )
                )
            }

            feeAmountData = SendModule.AmountData(primaryAmountInfo, secondaryAmountInfo)
        }
        loading = feeState.feeStatus == null
        emitState()
    }


    private val _sendTransactionSettingsFlow = MutableStateFlow(
        SendTransactionSettings.Common
    )
    override val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings> =
        _sendTransactionSettingsFlow.asStateFlow()

    private var feeAmountData: SendModule.AmountData? = null
    private var cautions: List<CautionViewItem> = listOf()
    private val sendable: Boolean
        get() = hasEnoughFeeAmount && amountState.canBeSend && addressState.canBeSend
    private var loading = true
    private var fields = listOf<DataField>()

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
            feeService.stateFlow.collect {
                handleUpdatedFeeState(it)
            }
        }
        coroutineScope.launch(Dispatchers.Default) {
            xRateService.getRateFlow(token.coin.uid).collect {
                coinRate = it
            }
        }
        coroutineScope.launch(Dispatchers.Default) {
            xRateService.getRateFlow(feeToken.coin.uid).collect {
                feeCoinRate = it
            }
        }

        // To calculate fee we need ton adapter
        coroutineScope.launch(Dispatchers.Default) {
            // Do not create adapter for hardware wallet account - don't want to display scan dialog
            if (accountManager.activeAccount?.isHardwareWalletAccount == true) {
                return@launch
            }
            walletUseCase.createWalletIfNotExists(feeToken)?.also {
                adapterManager.awaitAdapterForWallet<ISendTonAdapter>(it)
            }
        }
    }

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Ton)
        amountService.setAmount(data.amount)
        amountService.setMemo(data.memo)
        addressService.setAddress(addressHandlerTon.parseAddress(data.address))
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        try {
            val sdkBalance = adapterManager.getBalanceAdapterForWallet(wallet)
                ?.balanceData?.available ?: amountState.availableBalance
                ?: throw IllegalStateException("Balance unavailable")
            val draft = PendingTransactionDraft(
                wallet = wallet,
                token = token,
                amount = amountState.amount!!,
                fee = (feeState.feeStatus as? FeeStatus.Success)?.fee,
                sdkBalanceAtCreation = sdkBalance,
                fromAddress = "",  // TON doesn't require from address
                toAddress = addressState.address!!.hex,
                memo = amountState.memo,
                txHash = null
            )

            // 2. Register pending transaction
            pendingTxId = pendingRegistrar.register(draft)

            adapter.send(amountState.amount!!, addressState.tonAddress!!, amountState.memo)
            return SendTransactionResult.Ton(SendResult.Sent())
        } catch (e: Throwable) {
            pendingTxId?.let {
                pendingRegistrar.deleteFailed(it)
            }
            cautions = listOf(createCaution(e))
            emitState()
            throw e
        }
    }

    private fun checkFeeBalance(feeState: SendTonFeeService.State) {
        if (feeState.feeStatus !is FeeStatus.Success) {
            feeCaution = null
            hasEnoughFeeAmount = true
        }
        val fee = (feeState.feeStatus as? FeeStatus.Success)?.fee ?: return
        val availableBalance = adapterManager.getFeeTokenBalance(feeToken, token)
        if (availableBalance != null) {
            feeCaution = if (availableBalance < fee) {
                val formattedBalance = numberFormatter.formatCoinFull(
                    availableBalance,
                    null,
                    feeToken.decimals
                )
                createCaution(LocalizedException(R.string.not_enough_ton_for_fee, formattedBalance))
            } else {
                null
            }
            hasEnoughFeeAmount = feeCaution == null
        } else {
            val neededFee = fee.stripTrailingZeros()
                .toPlainString() + " " + feeToken.coin.code
            feeCaution = createCaution(
                LocalizedException(R.string.check_fee_warning, neededFee),
                type = CautionViewItem.Type.Warning
            )
        }
    }

    override fun hasSettings() = false

    @Composable
    override fun GetSettingsContent(navController: NavController) = Unit
}
