package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bitcoincore.models.SignedRawTransaction
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.core.IPegInMwebAdapter
import io.horizontalsystems.walletkit.core.ISendBitcoinAdapter
import io.horizontalsystems.walletkit.core.adapters.BitcoinBaseAdapter
import io.horizontalsystems.walletkit.core.adapters.LitecoinAdapter
import io.horizontalsystems.walletkit.modules.address.AddressHandlerMweb
import io.horizontalsystems.litecoinmwebkit.MwebFee
import io.horizontalsystems.litecoinmwebkit.MwebSendError
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.adapters.BitcoinFeeInfo
import io.horizontalsystems.walletkit.core.factories.FeeRateProviderFactory
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.entities.TransactionDataSortMode
import io.horizontalsystems.walletkit.modules.amount.AmountValidator
import io.horizontalsystems.walletkit.modules.evmfee.EvmSettingsInput
import io.horizontalsystems.walletkit.modules.fee.HSFee
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.send.bitcoin.rbfSupported
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.send.SendModule
import io.horizontalsystems.walletkit.modules.send.bitcoin.SendBitcoinAddressService
import io.horizontalsystems.walletkit.modules.send.bitcoin.SendBitcoinAmountService
import io.horizontalsystems.walletkit.modules.send.bitcoin.SendBitcoinFeeRateService
import io.horizontalsystems.walletkit.modules.send.bitcoin.SendBitcoinFeeService
import io.horizontalsystems.walletkit.modules.send.bitcoin.settings.SendBtcSettingsViewModel
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.InfoText
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.FeeRateCaution

class SendTransactionServiceBtc(
    private val token: Token,
    // A fee rate chosen on another instance's settings screen, applied once the recommended
    // rates are known (which would otherwise replace it).
    private val initialFeeRate: Int? = null,
) : AbstractSendTransactionService(true, false), ISignOnlySendTransactionService {
    private val adapter = App.adapterManager.getAdapterForToken<ISendBitcoinAdapter>(token)!!
    private val provider = FeeRateProviderFactory.provider(token.blockchainType)!!
    private val feeService = SendBitcoinFeeService(adapter)
    private val feeRateService = SendBitcoinFeeRateService(provider)
    private val amountService = SendBitcoinAmountService(adapter, token.coin.code, AmountValidator())
    private val addressService = SendBitcoinAddressService(adapter)

    private var feeRateState = feeRateService.stateFlow.value
    private var bitcoinFeeInfo = feeService.bitcoinFeeInfoFlow.value
    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    private var memo: String? = null
    private var changeToFirstInput: Boolean = false
    private var utxoFilters: UtxoFilters = UtxoFilters()
    private var unspentOutputs: List<UnspentOutputInfo>? = null
    private var transactionSorting: TransactionDataSortMode? = null
    private var rbfEnabled: Boolean = false
    private var pluginData: Map<Byte, IPluginData>? = null
    private var networkFee: SendModule.AmountData? = null
    // Until the initial fee rate is applied the state must not read as sendable: the recommended
    // rate lands first, and a send in that window would use it instead of the user's choice.
    private var feeRateInitialized = initialFeeRate == null

    private var fields = listOf<DataField>()

    // A Litecoin wallet paying an MWEB address is a peg-in, built and broadcast by the MWEB
    // kit of the same account; the address, amount and fee services above do not apply to it.
    private var pegIn: PegIn? = null

    private class PegIn(val address: String, val amount: BigDecimal) {
        var fee: BigDecimal? = null
        var error: HSCaution? = null
    }

    private val pegInAdapter: IPegInMwebAdapter? by lazy {
        App.coinManager.getToken(TokenQuery(BlockchainType.LitecoinMweb, TokenType.Native))
            ?.let { App.adapterManager.getAdapterForToken<IPegInMwebAdapter>(it) }
    }

    private val litecoinKit get() = (adapter as? LitecoinAdapter)?.kit

    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Btc())

    override fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            feeRateService.stateFlow.collect {
                handleFeeRateState(it)
            }
        }
        coroutineScope.launch {
            feeService.bitcoinFeeInfoFlow.collect {
                handleBitcoinFeeInfo(it)
            }
        }
        coroutineScope.launch {
            amountService.stateFlow.collect {
                handleAmountState(it)
            }
        }
        coroutineScope.launch {
            addressService.stateFlow.collect {
                handleAddressState(it)
            }
        }

        coroutineScope.launch {
            feeRateService.start()
            initialFeeRate?.let { feeRateService.setFeeRate(it) }
            feeRateInitialized = true
            emitState()
        }
    }

    // The amount, address and fee services are collected asynchronously, so this instance keeps
    // describing the previous deposit until they run; a replaced deposit gets a fresh instance at
    // the fee rate on screen.
    override fun fresh(): AbstractSendTransactionService = SendTransactionServiceBtc(token, feeRateState.feeRate)

    private fun handleAddressState(state: SendBitcoinAddressService.State) {
        addressState = state

        amountService.setValidAddress(addressState.validAddress)
        feeService.setValidAddress(addressState.validAddress)

        emitState()
    }

    private fun handleAmountState(state: SendBitcoinAmountService.State) {
        amountState = state

        feeService.setAmount(amountState.amount)

        emitState()
    }

    private fun handleBitcoinFeeInfo(info: BitcoinFeeInfo?) {
        bitcoinFeeInfo = info

        refreshNetworkFee()

        emitState()
    }

    // Only settings that differ from their defaults are worth a row: a timelock is off unless
    // chosen, and replace-by-fee is on unless switched off.
    private fun refreshFields() {
        val lockTime = (pluginData?.get(HodlerPlugin.id) as? HodlerData)?.lockTimeInterval

        fields = listOfNotNull(
            lockTime?.let { DataFieldBtcTimeLock(it) },
            if (token.blockchainType.rbfSupported && !rbfEnabled) DataFieldBtcRbfDisabled else null,
        )
    }

    private fun refreshNetworkFee() {
        networkFee = bitcoinFeeInfo?.fee?.let { fee ->
            getAmountData(CoinValue(token, fee))
        }
    }

    private suspend fun handleFeeRateState(state: SendBitcoinFeeRateService.State) {
        feeRateState = state

        feeService.setFeeRate(feeRateState.feeRate)
        amountService.setFeeRate(feeRateState.feeRate)
        refreshPegIn()
        // Published so a swap provider prices its route at the rate the transfer is sent with.
        sendTransactionSettingsFlow.value = SendTransactionSettings.Btc(feeRateState.feeRate)

        emitState()
    }

    // Fee-inclusive: computed by the adapter from the fee rate, recipient, memo and the
    // selected outputs, so it is the exact amount a whole-balance send can carry.
    override fun maxSendableAmount(): BigDecimal? {
        val pegIn = pegIn ?: return amountState.availableBalance
        val feeRate = feeRateState.feeRate ?: return null
        val mwebAdapter = pegInAdapter ?: return null
        // The Litecoin side pays the MWEB fee and the next block's HogEx input on top of its own fee.
        val available = adapter.availableBalance(feeRate, null, null, unspentOutputs, null, false, utxoFilters)
        val hogExInputFee = adapter.satoshiToBTC(feeRate.toLong() * MwebFee.HOGEX_INPUT_VBYTES)
        return (available - mwebAdapter.pegInMwebFee(feeRate) - hogExInputFee).coerceAtLeast(BigDecimal.ZERO)
    }

    private suspend fun refreshPegIn() {
        val pegIn = pegIn ?: return
        val feeRate = feeRateState.feeRate ?: return
        val kit = litecoinKit
        val mwebAdapter = pegInAdapter
        pegIn.fee = null
        networkFee = null
        if (kit == null || mwebAdapter == null) {
            pegIn.error = HSCaution(TranslatableString.ResString(R.string.Send_Error_EnableMwebWallet))
            return
        }
        try {
            val fee = mwebAdapter.estimatePegInFee(kit, pegIn.amount, feeRate)
            pegIn.fee = fee
            pegIn.error = null
            networkFee = getAmountData(CoinValue(token, fee))
        } catch (e: MwebSendError.InsufficientBalance) {
            pegIn.error = HSCaution(TranslatableString.ResString(R.string.Swap_ErrorInsufficientBalance))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            pegIn.error = HSCaution(TranslatableString.PlainString(e.message ?: e.javaClass.simpleName))
        }
    }

    private fun pegInState(pegIn: PegIn): SendTransactionServiceState {
        val sendable = pegIn.fee != null && pegIn.error == null && feeRateState.canBeSend && feeRateInitialized
        val hasError = pegIn.error != null || feeRateState.feeRateCaution?.isError() == true

        return SendTransactionServiceState(
            uuid = uuid,
            networkFee = networkFee,
            cautions = listOfNotNull(feeRateState.feeRateCaution, pegIn.error).map(HSCaution::toCautionViewItem),
            sendable = sendable,
            loading = !sendable && !hasError,
            fields = listOf(),
        )
    }

    override fun createState(): SendTransactionServiceState {
        pegIn?.let { return pegInState(it) }

        val sendable = amountState.canBeSend && feeRateState.canBeSend && addressState.canBeSend && feeRateInitialized

        val hasError = amountState.amountCaution?.isError() == true ||
                feeRateState.feeRateCaution?.isError() == true ||
                addressState.addressError != null

        val loading = !sendable && !hasError

        // The address error counts as an error caution too: a state that is neither sendable nor
        // carrying one reads as still loading, and a deposit to a rejected address would then
        // wait out the settle timeout instead of failing at once.
        val addressCaution = addressState.addressError?.let {
            HSCaution(TranslatableString.PlainString(it.message ?: it.javaClass.simpleName))
        }

        return SendTransactionServiceState(
            uuid = uuid,
            networkFee = networkFee,
            cautions = listOfNotNull(amountState.amountCaution, feeRateState.feeRateCaution, addressCaution).map(HSCaution::toCautionViewItem),
            sendable = sendable,
            loading = loading,
            fields = fields,
        )
    }

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Btc)

        unspentOutputs = data.unspentOutputs
        utxoFilters = data.utxoFilters
        if (token.blockchainType == BlockchainType.Litecoin && AddressHandlerMweb.isMwebAddress(data.address)) {
            pegIn = PegIn(data.address, data.amount)
            fields = listOf()
            refreshPegIn()
            emitState()
            return
        }
        pegIn = null

        memo = data.memo
        changeToFirstInput = data.changeToFirstInput
        unspentOutputs = data.unspentOutputs
        transactionSorting = data.transactionSorting
        rbfEnabled = data.rbfEnabled
        pluginData = data.pluginData

        data.recommendedGasRate?.let {
            feeRateService.setRecommendedAndMin(it, it)
        }

        feeService.setMemo(memo)
        feeService.setPluginData(pluginData)
        feeService.setChangeToFirstInput(changeToFirstInput)
        utxoFilters = data.utxoFilters
        feeService.setUtxoFilters(utxoFilters)
        feeService.setCustomUnspentOutputs(unspentOutputs)

        amountService.setMemo(memo)
        amountService.setPluginData(pluginData)
        amountService.setUserMinimumSendAmount(data.minimumSendAmount)
        amountService.setChangeToFirstInput(changeToFirstInput)
        amountService.setUtxoFilters(utxoFilters)
        amountService.setCustomUnspentOutputs(unspentOutputs)
        amountService.setAmount(data.amount)

        addressService.setPluginData(pluginData)
        addressService.setAddress(Address(data.address))

        refreshFields()
        emitState()
    }

    @Composable
    override fun GetSettingsContent(navigation: HSNavigation) {
        val sendSettingsViewModel = viewModel<SendBtcSettingsViewModel>(
            factory = SendBtcSettingsViewModel.Factory(feeRateService, feeService, token)
        )

        SendBtcFeeSettingsScreen(navigation, sendSettingsViewModel)
    }

    override fun signTransaction(): SignedTransactionInfo {
        check(pegIn == null) { "A peg-in is signed and broadcast by the MWEB kit in one step" }
        val signed = signRawTransaction()
        return SignedTransactionInfo(hex = signed.hex, transactionHash = signed.transactionHash)
    }

    private fun signRawTransaction(): SignedRawTransaction {
        return adapter.rawTransaction(
            amount = amountState.amount!!,
            address = addressState.validAddress?.hex!!,
            memo = memo,
            feeRate = feeRateState.feeRate!!,
            unspentOutputs = unspentOutputs,
            utxoFilters = utxoFilters,
        )
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult.Btc {
        pegIn?.let { pegIn ->
            val kit = litecoinKit ?: throw IllegalStateException("Litecoin kit is not available")
            val mwebAdapter = pegInAdapter ?: throw IllegalStateException("Litecoin MWEB wallet is not enabled")
            val txHash = mwebAdapter.pegIn(kit, pegIn.address, pegIn.amount, feeRateState.feeRate!!)
            // The kit registered the signed transaction with the Litecoin wallet before broadcasting.
            val record = kit.getTransaction(txHash)?.let { (adapter as BitcoinBaseAdapter).transactionRecord(it) }
            return SendTransactionResult.Btc(record)
        }

        val transactionRecord = adapter.send(
            amount = amountState.amount!!,
            address = addressState.validAddress?.hex!!,
            memo = memo,
            feeRate = feeRateState.feeRate!!,
            unspentOutputs = unspentOutputs,
            // Callers that must stay immediately spendable (a private send deposit) pass no
            // plugin data; a plain send may carry the user's timelock.
            pluginData = pluginData,
            transactionSorting = transactionSorting,
            rbfEnabled = rbfEnabled,
            changeToFirstInput = changeToFirstInput,
            utxoFilters = utxoFilters
        )

        return SendTransactionResult.Btc(transactionRecord)
    }
}

@Composable
fun SendBtcFeeSettingsScreen(
    navigation: HSNavigation,
    viewModel: SendBtcSettingsViewModel
) {
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.SendEvmSettings_Title),
        onBack = navigation::removeLastOrNull,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Reset),
                enabled = uiState.resetEnabled,
                tint = ComposeAppTheme.colors.jacob,
                onClick = {
                    viewModel.reset()
                }
            )
        )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            VSpacer(12.dp)
            HSFee(
                coinCode = viewModel.token.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fee = uiState.fee,
                rate = uiState.rate,
                navigation = navigation
            )

            if (viewModel.feeRateChangeable) {
                VSpacer(24.dp)
                EvmSettingsInput(
                    title = stringResource(R.string.FeeSettings_FeeRate),
                    info = stringResource(R.string.FeeSettings_FeeRate_Info),
                    value = uiState.feeRate?.toBigDecimal() ?: BigDecimal.ZERO,
                    decimals = 0,
                    caution = uiState.feeRateCaution,
                    navigation = navigation,
                    onValueChange = {
                        viewModel.updateFeeRate(it.toInt())
                    },
                    onClickIncrement = {
                        viewModel.incrementFeeRate()
                    },
                    onClickDecrement = {
                        viewModel.decrementFeeRate()
                    }
                )
                InfoText(
                    text = stringResource(R.string.FeeSettings_FeeRate_RecommendedInfo),
                )
            }

            uiState.feeRateCaution?.let {
                FeeRateCaution(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp
                    ),
                    feeRateCaution = it
                )
            }

            VSpacer(32.dp)
        }
    }
}