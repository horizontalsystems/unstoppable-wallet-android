package cash.p.terminal.modules.send.evm.confirmation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.ViewModelUiState
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.ethereum.EvmCoinServiceFactory
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.send.evm.SendEvmData
import cash.p.terminal.modules.sendevmtransaction.SectionViewItem
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SendEvmConfirmationViewModel(
    private val sendEvmTransactionViewItemFactory: SendEvmTransactionViewItemFactory,
    private val sendTransactionService: SendTransactionServiceEvm,
    private val transactionData: TransactionData,
    private val additionalInfo: SendEvmData.AdditionalInfo?
) : ViewModelUiState<SendEvmConfirmationUiState>() {
    private var sendTransactionState = sendTransactionService.stateFlow.value

    private val sectionViewItems = sendEvmTransactionViewItemFactory.getItems(
        transactionData,
        additionalInfo,
        sendTransactionService.decorate(transactionData)
    )

    override fun createState() = SendEvmConfirmationUiState(
        networkFee = sendTransactionState.networkFee,
        cautions = sendTransactionState.cautions,
        sendEnabled = sendTransactionState.sendable,
        transactionFields = sendTransactionState.fields,
        sectionViewItems = sectionViewItems
    )

    suspend fun send() = withContext(Dispatchers.Default) {
        sendTransactionService.sendTransaction()
    }

    init {
        viewModelScope.launch {
            sendTransactionService.stateFlow.collect { transactionState ->
                sendTransactionState = transactionState
                emitState()
            }
        }

        sendTransactionService.start(viewModelScope)

        sendTransactionService.setSendTransactionData(SendTransactionData.Evm(transactionData, null))
    }

    class Factory(
        private val transactionData: TransactionData,
        private val additionalInfo: SendEvmData.AdditionalInfo?,
        private val blockchainType: BlockchainType
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val sendTransactionService = SendTransactionServiceEvm(blockchainType)
            val feeToken = App.evmBlockchainManager.getBaseToken(blockchainType)!!
            val coinServiceFactory = EvmCoinServiceFactory(
                feeToken,
                App.marketKit,
                App.currencyManager,
                App.coinManager
            )

            val sendEvmTransactionViewItemFactory = SendEvmTransactionViewItemFactory(
                App.evmLabelManager,
                coinServiceFactory,
                App.contactsRepository,
                blockchainType
            )

            return SendEvmConfirmationViewModel(
                sendEvmTransactionViewItemFactory,
                sendTransactionService,
                transactionData,
                additionalInfo
            ) as T
        }
    }

}

data class SendEvmConfirmationUiState(
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val sendEnabled: Boolean,
    val transactionFields: List<DataField>,
    val sectionViewItems: List<SectionViewItem>
)