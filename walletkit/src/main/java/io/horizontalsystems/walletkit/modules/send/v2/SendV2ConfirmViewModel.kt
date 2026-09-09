package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.core.LocalizedException
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.managers.RecentAddressManager
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.contacts.ContactsRepository
import io.horizontalsystems.walletkit.modules.contacts.model.Contact
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionResult
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.send.SendResult
import io.horizontalsystems.walletkit.modules.xrate.XRateService
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Coin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

data class SendV2ConfirmUiState(
    val wallet: Wallet,
    val amount: BigDecimal,
    val address: Address,
    val contact: Contact?,
    val memo: String?,
    val rate: CurrencyValue?,
    val fee: BigDecimal?,
    val feeCoin: Coin,
    val feeCoinDecimals: Int,
    val feeCoinRate: CurrencyValue?,
    val fields: List<DataField>,
    val cautions: List<CautionViewItem>,
    val sendable: Boolean,
    val loading: Boolean,
    val hasSettings: Boolean,
    val hasNonceSettings: Boolean,
    val error: Throwable?,
)

/**
 * Confirmation of a plain transfer for any blockchain type. The chain's send-transaction
 * service owns the fee, its settings, cautions and the send itself; this view model only
 * feeds it the transfer and presents its state.
 */
class SendV2ConfirmViewModel(
    private val wallet: Wallet,
    private val amount: BigDecimal,
    private val address: Address,
    private val memo: String?,
    val sendTransactionService: AbstractSendTransactionService,
    private val xRateService: XRateService,
    private val contactsRepository: ContactsRepository,
    private val recentAddressManager: RecentAddressManager,
) : ViewModelUiState<SendV2ConfirmUiState>() {

    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val token = wallet.token
    private var serviceState = sendTransactionService.stateFlow.value
    private var rate = xRateService.getRate(token.coin.uid)
    private var feeCoinRate: CurrencyValue? = null
    private var error: Throwable? = null
    private val contact = contactsRepository.getContactsFiltered(
        token.blockchainType,
        addressQuery = address.hex
    ).firstOrNull()

    init {
        viewModelScope.launch {
            xRateService.getRateFlow(token.coin.uid).collect {
                rate = it
                refreshFeeCoinRate()
                emitState()
            }
        }
        viewModelScope.launch {
            sendTransactionService.stateFlow.collect {
                serviceState = it
                refreshFeeCoinRate()
                emitState()
            }
        }

        refreshFeeCoinRate()
        sendTransactionService.start(viewModelScope)

        viewModelScope.launch {
            try {
                val data = ChainRegistry[token.blockchainType]
                    ?.sendTransactionData(token, amount, address.hex, memo)
                    ?: throw UnsupportedOperationException(token.blockchainType.uid)
                sendTransactionService.setSendTransactionData(data)
            } catch (e: Throwable) {
                error = e
                emitState()
            }
        }
    }

    private fun feeCoinValue() = serviceState.networkFee?.primary?.coinValue
    private fun feeCoin(): Coin = feeCoinValue()?.coin ?: token.coin

    // The fee is not always paid in the sent token (ERC20 fees are in the chain's coin), so
    // its rate is looked up separately once the service reports the fee coin.
    private fun refreshFeeCoinRate() {
        val feeCoin = feeCoin()
        feeCoinRate = if (feeCoin.uid == token.coin.uid) rate else xRateService.getRate(feeCoin.uid)
    }

    override fun createState() = SendV2ConfirmUiState(
        wallet = wallet,
        amount = amount,
        address = address,
        contact = contact,
        memo = memo,
        rate = rate,
        fee = serviceState.networkFee?.primary?.coinValue?.value,
        feeCoin = feeCoin(),
        feeCoinDecimals = feeCoinValue()?.decimal ?: token.decimals,
        feeCoinRate = feeCoinRate,
        fields = serviceState.fields,
        cautions = serviceState.cautions,
        sendable = serviceState.sendable,
        loading = serviceState.loading,
        hasSettings = sendTransactionService.hasSettings,
        hasNonceSettings = sendTransactionService.hasNonceSettings,
        error = error,
    )

    fun onClickSend() {
        viewModelScope.launch(Dispatchers.IO) {
            send()
        }
    }

    private suspend fun send() = withContext(Dispatchers.IO) {
        sendResult = SendResult.Sending
        try {
            val result = sendTransactionService.sendTransaction()
            sendResult = SendResult.Sent(txHash = txHash(result))
            recentAddressManager.setRecentAddress(address, token.blockchainType)
        } catch (e: Throwable) {
            sendResult = SendResult.Failed(createCaution(e))
        }
    }

    private fun txHash(result: SendTransactionResult): String? = when (result) {
        is SendTransactionResult.Evm -> result.transactionHash
        is SendTransactionResult.Btc -> result.transactionRecord?.transactionHash
        is SendTransactionResult.Tron -> result.txHash
        is SendTransactionResult.Stellar -> result.txHash
        is SendTransactionResult.Solana -> result.txHash
        is SendTransactionResult.Zcash -> result.transactionHash
        is SendTransactionResult.Monero -> result.txHash
        is SendTransactionResult.Thorchain -> result.txHash
        is SendTransactionResult.Zano -> result.txHash
        SendTransactionResult.Ton -> null
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: error.javaClass.simpleName))
    }

    class Factory(private val input: SendV2ConfirmPage.Input) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendV2ConfirmViewModel(
                wallet = input.wallet,
                amount = input.amount,
                address = input.address,
                memo = input.memo,
                sendTransactionService = SendTransactionServiceFactory.create(input.wallet.token),
                xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency),
                contactsRepository = App.contactsRepository,
                recentAddressManager = App.recentAddressManager,
            ) as T
        }
    }
}
