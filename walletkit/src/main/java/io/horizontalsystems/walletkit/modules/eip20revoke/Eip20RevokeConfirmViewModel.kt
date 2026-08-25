package io.horizontalsystems.walletkit.modules.eip20revoke

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
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.modules.contacts.ContactsRepository
import io.horizontalsystems.walletkit.modules.contacts.model.Contact
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
        viewModelScope.launch {
            sendTransactionService.setSendTransactionData(
                SendTransactionData.Tron.Trc20Approve(spenderAddress, BigDecimal.ZERO)
            )
        }
    }

    private fun prepareEvmRevokeTransaction() {
        val eip20Adapter =
            walletManager.activeWallets.firstOrNull { it.token == token }?.let { wallet ->
                adapterManager.getAdapterForWallet<IEip20ApproveAdapter>(wallet)
            } ?: throw IllegalStateException("Eip20Adapter not found for token")
        viewModelScope.launch {
            val transactionData =
                eip20Adapter.buildRevokeTransactionData(spenderAddress)
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
