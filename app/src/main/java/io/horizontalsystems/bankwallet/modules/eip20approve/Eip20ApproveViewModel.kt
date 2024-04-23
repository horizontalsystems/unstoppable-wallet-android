package io.horizontalsystems.bankwallet.modules.eip20approve

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.eip20approve.AllowanceMode.OnlyRequired
import io.horizontalsystems.bankwallet.modules.eip20approve.AllowanceMode.Unlimited
import io.horizontalsystems.bankwallet.modules.multiswap.FiatService
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class Eip20ApproveViewModel(
    private val token: Token,
    private val requiredAllowance: BigDecimal,
    private val spenderAddress: String,
    private val walletManager: IWalletManager,
    private val adapterManager: IAdapterManager,
    val sendTransactionService: SendTransactionServiceEvm,
    private val currencyManager: CurrencyManager,
    private val fiatService: FiatService,
    private val contactsRepository: ContactsRepository,
) : ViewModelUiState<Eip20ApproveUiState>() {
    private val currency = currencyManager.baseCurrency
    private var allowanceMode = OnlyRequired
    private var sendTransactionState = sendTransactionService.stateFlow.value
    private var fiatAmount: BigDecimal? = null
    private val contact = contactsRepository.getContactsFiltered(
        blockchainType = token.blockchainType,
        addressQuery = spenderAddress
    ).firstOrNull()

    override fun createState() = Eip20ApproveUiState(
        token = token,
        requiredAllowance = requiredAllowance,
        allowanceMode = allowanceMode,
        networkFee = sendTransactionState.networkFee,
        cautions = sendTransactionState.cautions,
        currency = currency,
        fiatAmount = fiatAmount,
        spenderAddress = spenderAddress,
        contact = contact,
        approveEnabled = sendTransactionState.sendable
    )

    init {

        fiatService.setCurrency(currency)
        fiatService.setToken(token)
        fiatService.setAmount(requiredAllowance)

        viewModelScope.launch {
            fiatService.stateFlow.collect {
                fiatAmount = it.fiatAmount
                emitState()
            }
        }

        viewModelScope.launch {
            sendTransactionService.stateFlow.collect { transactionState ->
                sendTransactionState = transactionState
                emitState()
            }
        }

        sendTransactionService.start(viewModelScope)
    }

    fun setAllowanceMode(allowanceMode: AllowanceMode) {
        this.allowanceMode = allowanceMode

        emitState()
    }

    fun freeze() {
        val eip20Adapter =
            walletManager.activeWallets.firstOrNull { it.token == token }?.let { wallet ->
                adapterManager.getAdapterForWallet(wallet) as? Eip20Adapter
            }

        checkNotNull(eip20Adapter)

        val transactionData = when (allowanceMode) {
            OnlyRequired -> eip20Adapter.buildApproveTransactionData(
                Address(spenderAddress),
                requiredAllowance
            )

            Unlimited -> eip20Adapter.buildApproveUnlimitedTransactionData(Address(spenderAddress))
        }

        sendTransactionService.setSendTransactionData(SendTransactionData.Evm(transactionData, null))
    }

    suspend fun approve() = withContext(Dispatchers.Default) {
        sendTransactionService.sendTransaction()
    }

    class Factory(
        private val token: Token,
        private val requiredAllowance: BigDecimal,
        private val spenderAddress: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val sendTransactionService = SendTransactionServiceEvm(token.blockchainType)

            return Eip20ApproveViewModel(
                token,
                requiredAllowance,
                spenderAddress,
                App.walletManager,
                App.adapterManager,
                sendTransactionService,
                App.currencyManager,
                FiatService(App.marketKit),
                App.contactsRepository
            ) as T
        }
    }
}

data class Eip20ApproveUiState(
    val token: Token,
    val requiredAllowance: BigDecimal,
    val allowanceMode: AllowanceMode,
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val currency: Currency,
    val fiatAmount: BigDecimal?,
    val spenderAddress: String,
    val contact: Contact?,
    val approveEnabled: Boolean,
)

enum class AllowanceMode {
    OnlyRequired, Unlimited
}

