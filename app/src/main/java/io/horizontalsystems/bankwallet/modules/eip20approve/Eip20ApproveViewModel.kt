package io.horizontalsystems.bankwallet.modules.eip20approve

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.core.adapters.Trc20Adapter
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.isEvm
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.eip20approve.AllowanceMode.OnlyRequired
import io.horizontalsystems.bankwallet.modules.eip20approve.AllowanceMode.Unlimited
import io.horizontalsystems.bankwallet.modules.multiswap.FiatService
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceState
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

@HiltViewModel(assistedFactory = Eip20ApproveViewModel.Factory::class)
class Eip20ApproveViewModel @AssistedInject constructor(
    @Assisted private val token: Token,
    @Assisted private val requiredAllowance: BigDecimal,
    @Assisted private val spenderAddress: String,
    private val adapterManager: IAdapterManager,
    private val currencyManager: CurrencyManager,
    marketKit: MarketKitWrapper,
    private val contactsRepository: ContactsRepository,
) : ViewModelUiState<Eip20ApproveUiState>() {
    val sendTransactionService: AbstractSendTransactionService
    private val fiatService: FiatService

    private val currency = currencyManager.baseCurrency
    private var allowanceMode = OnlyRequired
    private var initialLoading = true
    private var sendTransactionState: SendTransactionServiceState
    private var fiatAmount: BigDecimal? = null
    private val contact = contactsRepository.getContactsFiltered(
        blockchainType = token.blockchainType,
        addressQuery = spenderAddress
    ).firstOrNull()

    init {
        sendTransactionService = SendTransactionServiceFactory.create(token)
        fiatService = FiatService(marketKit)
        sendTransactionState = sendTransactionService.stateFlow.value

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
        val eip20Adapter = adapterManager.getAdapterForToken<Eip20Adapter>(token)
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

    private suspend fun freezeTron() {
        val trc20Adapter = adapterManager.getAdapterForToken<Trc20Adapter>(token)
        checkNotNull(trc20Adapter)

        val triggerSmartContract = when (allowanceMode) {
            OnlyRequired -> trc20Adapter.approveTrc20TriggerSmartContract(
                spenderAddress,
                requiredAllowance
            )

            Unlimited -> trc20Adapter.approveTrc20TriggerSmartContractUnlim(spenderAddress)
        }

        sendTransactionService.setSendTransactionData(SendTransactionData.Tron.WithContract(triggerSmartContract))
    }

    suspend fun approve() = withContext(Dispatchers.Default) {
        sendTransactionService.sendTransaction()
    }

    @AssistedFactory
    interface Factory {
        fun create(token: Token, requiredAllowance: BigDecimal, spenderAddress: String): Eip20ApproveViewModel
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

