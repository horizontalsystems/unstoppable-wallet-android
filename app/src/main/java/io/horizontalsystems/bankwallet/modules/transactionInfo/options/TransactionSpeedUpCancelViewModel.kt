package io.horizontalsystems.bankwallet.modules.transactionInfo.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceState
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

class TransactionSpeedUpCancelViewModel(
    val sendTransactionService: SendTransactionServiceEvm,
    private val transactionHash: String,
    private val evmKitWrapper: EvmKitWrapper,
    private val optionType: SpeedUpCancelType,
    private val sendEvmTransactionViewItemFactory: SendEvmTransactionViewItemFactory
) : ViewModelUiState<TransactionSpeedUpCancelUiState>() {

    val title: String = when (optionType) {
        SpeedUpCancelType.SpeedUp -> Translator.getString(R.string.TransactionInfoOptions_SpeedUp_Title)
        SpeedUpCancelType.Cancel -> Translator.getString(R.string.TransactionInfoOptions_Cancel_Title)
    }

    val buttonTitle: String = when (optionType) {
        SpeedUpCancelType.SpeedUp -> Translator.getString(R.string.TransactionInfoOptions_SpeedUp_Button)
        SpeedUpCancelType.Cancel -> Translator.getString(R.string.TransactionInfoOptions_Cancel_Button)
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