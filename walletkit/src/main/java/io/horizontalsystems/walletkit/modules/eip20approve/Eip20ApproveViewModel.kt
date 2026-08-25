package io.horizontalsystems.walletkit.modules.eip20approve

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.IEip20ApproveAdapter
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.isEvm
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.modules.contacts.ContactsRepository
import io.horizontalsystems.walletkit.modules.contacts.model.Contact
import io.horizontalsystems.walletkit.modules.eip20approve.AllowanceMode.OnlyRequired
import io.horizontalsystems.walletkit.modules.eip20approve.AllowanceMode.Unlimited
import io.horizontalsystems.walletkit.modules.multiswap.FiatService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.walletkit.modules.send.SendModule
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class Eip20ApproveViewModel(
    private val token: Token,
    private val requiredAllowance: BigDecimal,
    private val spenderAddress: String,
    private val adapterManager: IAdapterManager,
    val sendTransactionService: AbstractSendTransactionService,
    private val currencyManager: CurrencyManager,
    private val fiatService: FiatService,
    private val contactsRepository: ContactsRepository,
) : ViewModelUiState<Eip20ApproveUiState>() {
    private val currency = currencyManager.baseCurrency
    private var allowanceMode = OnlyRequired
    private var initialLoading = true
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
        approveEnabled = sendTransactionState.sendable,
        initialLoading = initialLoading,
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
                initialLoading = initialLoading && transactionState.loading

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
        viewModelScope.launch {
            if (token.blockchainType.isEvm) {
                freezeEvm()
            } else if (token.blockchainType == BlockchainType.Tron) {
                freezeTron()
            }
        }
    }

    private suspend fun freezeEvm() {
        val eip20Adapter = adapterManager.getAdapterForToken<IEip20ApproveAdapter>(token)
        checkNotNull(eip20Adapter)

        val transactionData = when (allowanceMode) {
            OnlyRequired -> eip20Adapter.buildApproveTransactionData(
                spenderAddress,
                requiredAllowance
            )

            Unlimited -> eip20Adapter.buildApproveUnlimitedTransactionData(spenderAddress)
        }

        sendTransactionService.setSendTransactionData(SendTransactionData.Evm(transactionData, null))
    }

    private suspend fun freezeTron() {
        val amount = when (allowanceMode) {
            OnlyRequired -> requiredAllowance
            Unlimited -> null
        }

        sendTransactionService.setSendTransactionData(SendTransactionData.Tron.Trc20Approve(spenderAddress, amount))
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
            val sendTransactionService = SendTransactionServiceFactory.create(token)

            return Eip20ApproveViewModel(
                token,
                requiredAllowance,
                spenderAddress,
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
    val initialLoading: Boolean,
)

enum class AllowanceMode {
    OnlyRequired, Unlimited
}

