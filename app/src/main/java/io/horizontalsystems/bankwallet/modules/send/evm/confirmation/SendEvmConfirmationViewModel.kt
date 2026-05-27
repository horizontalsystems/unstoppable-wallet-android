package io.horizontalsystems.bankwallet.modules.send.evm.confirmation

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.RecentAddressManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceState
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import io.horizontalsystems.erc20kit.decorations.OutgoingEip20Decoration
import io.horizontalsystems.ethereumkit.decorations.OutgoingDecoration
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nftkit.decorations.OutgoingEip1155Decoration
import io.horizontalsystems.nftkit.decorations.OutgoingEip721Decoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = SendEvmConfirmationViewModel.Factory::class)
class SendEvmConfirmationViewModel @AssistedInject constructor(
    @Assisted private val transactionData: TransactionData,
    @Assisted private val additionalInfo: SendEvmData.AdditionalInfo?,
    @Assisted private val blockchainType: BlockchainType,
    evmBlockchainManager: EvmBlockchainManager,
    marketKit: MarketKitWrapper,
    currencyManager: CurrencyManager,
    coinManager: ICoinManager,
    evmLabelManager: EvmLabelManager,
    contactsRepo: ContactsRepository,
    private val recentAddressManager: RecentAddressManager,
) : ViewModelUiState<SendEvmConfirmationUiState>() {

    val sendTransactionService: SendTransactionServiceEvm
    private val sendEvmTransactionViewItemFactory: SendEvmTransactionViewItemFactory

    private var initialLoading = true
    private var sendTransactionState: SendTransactionServiceState

    private val transactionDecoration: TransactionDecoration?
    private val sectionViewItems: List<SectionViewItem>

    init {
        sendTransactionService = SendTransactionServiceEvm(blockchainType)

        val feeToken = evmBlockchainManager.getBaseToken(blockchainType)!!
        val coinServiceFactory = EvmCoinServiceFactory(feeToken, marketKit, currencyManager, coinManager)
        sendEvmTransactionViewItemFactory = SendEvmTransactionViewItemFactory(
            evmLabelManager,
            coinServiceFactory,
            contactsRepo,
            blockchainType
        )

        sendTransactionState = sendTransactionService.stateFlow.value
        transactionDecoration = sendTransactionService.decorate(transactionData)
        sectionViewItems = sendEvmTransactionViewItemFactory.getItems(
            transactionData,
            additionalInfo,
            transactionDecoration
        )

        viewModelScope.launch {
            sendTransactionService.stateFlow.collect { transactionState ->
                sendTransactionState = transactionState
                initialLoading = initialLoading && transactionState.loading
                emitState()
            }
        }

        sendTransactionService.start(viewModelScope)

        viewModelScope.launch {
            sendTransactionService.setSendTransactionData(SendTransactionData.Evm(transactionData, null))
        }
    }

    override fun createState() = SendEvmConfirmationUiState(
        networkFee = sendTransactionState.networkFee,
        cautions = sendTransactionState.cautions,
        sendEnabled = sendTransactionState.sendable,
        transactionFields = sendTransactionState.fields,
        sectionViewItems = sectionViewItems,
        initialLoading = initialLoading,
    )

    suspend fun send() = withContext(Dispatchers.Default) {
        sendTransactionService.sendTransaction()

        val address = when (transactionDecoration) {
            is OutgoingEip20Decoration -> transactionDecoration.to.eip55
            is OutgoingEip721Decoration -> transactionDecoration.to.eip55
            is OutgoingEip1155Decoration -> transactionDecoration.to.eip55
            is OutgoingDecoration -> transactionDecoration.to.eip55
            else -> null
        }
        address?.let {
            recentAddressManager.setRecentAddress(Address(address), blockchainType)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            transactionData: TransactionData,
            additionalInfo: SendEvmData.AdditionalInfo?,
            blockchainType: BlockchainType,
        ): SendEvmConfirmationViewModel
    }
}

data class SendEvmConfirmationUiState(
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val sendEnabled: Boolean,
    val transactionFields: List<DataField>,
    val sectionViewItems: List<SectionViewItem>,
    val initialLoading: Boolean
)
