package io.horizontalsystems.core.modules.eip20revoke

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.core.IAdapterManager
import io.horizontalsystems.core.core.ViewModelUiState
import io.horizontalsystems.core.core.adapters.Eip20Adapter
import io.horizontalsystems.core.core.adapters.Trc20Adapter
import io.horizontalsystems.core.core.ethereum.CautionViewItem
import io.horizontalsystems.core.core.isEvm
import io.horizontalsystems.core.core.managers.CurrencyManager
import io.horizontalsystems.core.core.managers.WalletManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.modules.contacts.ContactsRepository
import io.horizontalsystems.core.modules.contacts.model.Contact
import io.horizontalsystems.core.modules.multiswap.FiatService
import io.horizontalsystems.core.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.core.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.core.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.core.modules.send.SendModule
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID

class Eip20RevokeConfirmViewModel(
    private val token: Token,
    private val allowance: BigDecimal,
    private val spenderAddress: String,
    private val walletManager: WalletManager,
    private val adapterManager: IAdapterManager,
    val sendTransactionService: AbstractSendTransactionService,
    private val currencyManager: CurrencyManager,
    private val fiatService: FiatService,
    private val contactsRepository: ContactsRepository,
) : ViewModelUiState<Eip20RevokeUiState>() {
    private val currency = currencyManager.baseCurrency
    private var initialLoading = true
    private var sendTransactionState = sendTransactionService.stateFlow.value
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

    class Factory(
        private val token: Token,
        private val spenderAddress: String,
        private val allowance: BigDecimal,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val sendTransactionService = SendTransactionServiceFactory.create(token)//SendTransactionServiceEvm(token.blockchainType)

            return Eip20RevokeConfirmViewModel(
                token,
                allowance,
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
