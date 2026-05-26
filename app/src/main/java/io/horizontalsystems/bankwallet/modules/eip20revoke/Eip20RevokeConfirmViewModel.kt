package io.horizontalsystems.bankwallet.modules.eip20revoke

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
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
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
import java.util.UUID

@HiltViewModel(assistedFactory = Eip20RevokeConfirmViewModel.Factory::class)
class Eip20RevokeConfirmViewModel @AssistedInject constructor(
    @Assisted private val token: Token,
    @Assisted private val spenderAddress: String,
    @Assisted private val allowance: BigDecimal,
    private val walletManager: WalletManager,
    private val adapterManager: IAdapterManager,
    private val currencyManager: CurrencyManager,
    marketKit: MarketKitWrapper,
    private val contactsRepository: ContactsRepository,
) : ViewModelUiState<Eip20RevokeUiState>() {
    val sendTransactionService: AbstractSendTransactionService
    private val fiatService: FiatService

    private val currency = currencyManager.baseCurrency
    private var initialLoading = true
    private var sendTransactionState: SendTransactionServiceState
    private var fiatAmount: BigDecimal? = null
    private val contact = contactsRepository.getContactsFiltered(
        blockchainType = token.blockchainType,
        addressQuery = spenderAddress
    ).firstOrNull()

    override fun createState() = Eip20RevokeUiState(
        token = token,
        allowance = allowance,
        networkFee = sendTransactionState.networkFee,
        cautions = sendTransactionState.cautions,
        currency = currency,
        fiatAmount = fiatAmount,
        spenderAddress = spenderAddress,
        contact = contact,
        revokeEnabled = sendTransactionState.sendable,
        initialLoading = initialLoading,
    )

    val uuid = UUID.randomUUID().toString()

    init {
        sendTransactionService = SendTransactionServiceFactory.create(token)
        fiatService = FiatService(marketKit)
        sendTransactionState = sendTransactionService.stateFlow.value

        fiatService.setCurrency(currency)
        fiatService.setToken(token)
        fiatService.setAmount(allowance)

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

        when {
            token.blockchainType.isEvm -> prepareEvmRevokeTransaction()
            token.blockchainType == BlockchainType.Tron -> prepareTronRevokeTransaction()
            else -> throw IllegalArgumentException("Unsupported blockchain type for EIP-20 revoke")
        }
    }

    private fun prepareTronRevokeTransaction() {
        val trc20Adapter = adapterManager.getAdapterForToken<Trc20Adapter>(token)
            ?: throw IllegalStateException("Trc20Adapter not found for token")
        viewModelScope.launch {
            val triggerSmartContract =
                trc20Adapter.approveTrc20TriggerSmartContract(spenderAddress, BigDecimal.ZERO)
            sendTransactionService.setSendTransactionData(
                SendTransactionData.Tron.WithContract(triggerSmartContract)
            )
        }
    }

    private fun prepareEvmRevokeTransaction() {
        val eip20Adapter =
            walletManager.activeWallets.firstOrNull { it.token == token }?.let { wallet ->
                adapterManager.getAdapterForWallet<Eip20Adapter>(wallet)
            } ?: throw IllegalStateException("Eip20Adapter not found for token")
        viewModelScope.launch {
            val transactionData =
                eip20Adapter.buildRevokeTransactionData(Address(spenderAddress))
            sendTransactionService.setSendTransactionData(
                SendTransactionData.Evm(transactionData, null)
            )
        }
    }

    suspend fun revoke() = withContext(Dispatchers.Default) {
        sendTransactionService.sendTransaction()
    }

    @AssistedFactory
    interface Factory {
        fun create(token: Token, spenderAddress: String, allowance: BigDecimal): Eip20RevokeConfirmViewModel
    }
}

data class Eip20RevokeUiState(
    val token: Token,
    val allowance: BigDecimal,
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val currency: Currency,
    val fiatAmount: BigDecimal?,
    val spenderAddress: String,
    val contact: Contact?,
    val revokeEnabled: Boolean,
    val initialLoading: Boolean,
)
