package cash.p.terminal.modules.transactionInfo.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import io.horizontalsystems.core.ViewModelUiState
import cash.p.terminal.core.ethereum.EvmCoinServiceFactory
import cash.p.terminal.core.managers.EvmKitWrapper
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.services.SendTransactionServiceEvm
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionServiceState
import cash.p.terminal.modules.sendevmtransaction.SectionViewItem
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

internal class TransactionSpeedUpCancelViewModel(
    val sendTransactionService: SendTransactionServiceEvm,
    private val transactionHash: String,
    private val evmKitWrapper: EvmKitWrapper,
    private val optionType: SpeedUpCancelType,
    private val sendEvmTransactionViewItemFactory: SendEvmTransactionViewItemFactory
) : ViewModelUiState<TransactionSpeedUpCancelUiState>() {

    val title: String = when (optionType) {
        SpeedUpCancelType.SpeedUp -> cash.p.terminal.strings.helpers.Translator.getString(R.string.TransactionInfoOptions_SpeedUp_Title)
        SpeedUpCancelType.Cancel -> cash.p.terminal.strings.helpers.Translator.getString(R.string.TransactionInfoOptions_Cancel_Title)
    }

    val buttonTitle: String = when (optionType) {
        SpeedUpCancelType.SpeedUp -> cash.p.terminal.strings.helpers.Translator.getString(R.string.TransactionInfoOptions_SpeedUp_Button)
        SpeedUpCancelType.Cancel -> cash.p.terminal.strings.helpers.Translator.getString(R.string.TransactionInfoOptions_Cancel_Button)
    }

    private var sendTransactionState: SendTransactionServiceState = sendTransactionService.stateFlow.value
    private var error: Throwable? = null
    private var sectionViewItems: List<SectionViewItem> = listOf()

    override fun createState() = TransactionSpeedUpCancelUiState(
        sendTransactionState = sendTransactionState,
        sectionViewItems = sectionViewItems,
        error = error,
        sendEnabled = error == null && sendTransactionState.sendable
    )

    init {
        val fullTransaction = evmKitWrapper.evmKit
            .getFullTransactions(listOf(transactionHash.hexStringToByteArray()))
            .first()

        fullTransaction.transaction.nonce?.let {
            sendTransactionService.fixNonce(it)
        }

        if (fullTransaction.transaction.blockNumber != null) {
            error = TransactionAlreadyInBlock()

            emitState()
        } else {
            val transactionData = when (optionType) {
                SpeedUpCancelType.SpeedUp -> {
                    val transaction = fullTransaction.transaction
                    TransactionData(transaction.to!!, transaction.value!!, transaction.input!!)
                }

                SpeedUpCancelType.Cancel -> {
                    TransactionData(
                        evmKitWrapper.evmKit.receiveAddress,
                        BigInteger.ZERO,
                        byteArrayOf()
                    )
                }
            }

            sectionViewItems = sendEvmTransactionViewItemFactory.getItems(
                transactionData,
                null,
                sendTransactionService.decorate(transactionData)
            )
            emitState()

            viewModelScope.launch {
                sendTransactionService.stateFlow.collect { transactionState ->
                    sendTransactionState = transactionState
                    emitState()
                }
            }

            sendTransactionService.start(viewModelScope)
            sendTransactionService.setSendTransactionData(SendTransactionData.Evm(transactionData, null))
        }
    }

    suspend fun send() = withContext(Dispatchers.Default) {
        sendTransactionService.sendTransaction()
    }

    class Factory(
        private val blockchainType: BlockchainType,
        private val transactionHash: String,
        private val optionType: SpeedUpCancelType,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val feeToken = App.evmBlockchainManager.getBaseToken(blockchainType)!!
            val sendTransactionService = SendTransactionServiceEvm(feeToken)
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

            val evmKitWrapper =
                App.evmBlockchainManager.getEvmKitManager(blockchainType).evmKitWrapper!!

            return TransactionSpeedUpCancelViewModel(
                sendTransactionService,
                transactionHash,
                evmKitWrapper,
                optionType,
                sendEvmTransactionViewItemFactory
            ) as T
        }
    }
}

data class TransactionSpeedUpCancelUiState(
    val sendTransactionState: SendTransactionServiceState,
    val sectionViewItems: List<SectionViewItem>,
    val error: Throwable?,
    val sendEnabled: Boolean
)

class TransactionAlreadyInBlock : Exception()