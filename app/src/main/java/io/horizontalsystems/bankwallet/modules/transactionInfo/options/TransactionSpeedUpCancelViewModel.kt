package io.horizontalsystems.bankwallet.modules.transactionInfo.options

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
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

@HiltViewModel(assistedFactory = TransactionSpeedUpCancelViewModel.Factory::class)
class TransactionSpeedUpCancelViewModel @AssistedInject constructor(
    @Assisted private val transactionHash: String,
    @Assisted private val optionType: SpeedUpCancelType,
    @Assisted blockchainType: BlockchainType,
    evmBlockchainManager: EvmBlockchainManager,
    marketKit: MarketKitWrapper,
    currencyManager: CurrencyManager,
    coinManager: ICoinManager,
    evmLabelManager: EvmLabelManager,
    contactsRepository: ContactsRepository,
) : ViewModelUiState<TransactionSpeedUpCancelUiState>() {

    private val evmKitWrapper: EvmKitWrapper
    private val sendEvmTransactionViewItemFactory: SendEvmTransactionViewItemFactory
    val sendTransactionService: SendTransactionServiceEvm

    val title: String = when (optionType) {
        SpeedUpCancelType.SpeedUp -> Translator.getString(R.string.TransactionInfoOptions_SpeedUp_Title)
        SpeedUpCancelType.Cancel -> Translator.getString(R.string.TransactionInfoOptions_Cancel_Title)
    }

    val buttonTitle: String = when (optionType) {
        SpeedUpCancelType.SpeedUp -> Translator.getString(R.string.TransactionInfoOptions_SpeedUp_Button)
        SpeedUpCancelType.Cancel -> Translator.getString(R.string.TransactionInfoOptions_Cancel_Button)
    }

    private var initialLoading = true
    private var sendTransactionState: SendTransactionServiceState
    private var error: Throwable? = null
    private var sectionViewItems: List<SectionViewItem> = listOf()

    override fun createState() = TransactionSpeedUpCancelUiState(
        sendTransactionState = sendTransactionState,
        sectionViewItems = sectionViewItems,
        error = error,
        sendEnabled = error == null && sendTransactionState.sendable,
        initialLoading = initialLoading
    )

    init {
        val feeToken = evmBlockchainManager.getBaseToken(blockchainType)!!
        val coinServiceFactory = EvmCoinServiceFactory(feeToken, marketKit, currencyManager, coinManager)
        sendEvmTransactionViewItemFactory = SendEvmTransactionViewItemFactory(
            evmLabelManager, coinServiceFactory, contactsRepository, blockchainType
        )
        evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchainType).evmKitWrapper!!
        sendTransactionService = SendTransactionServiceEvm(blockchainType)
        sendTransactionState = sendTransactionService.stateFlow.value

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
                    initialLoading = initialLoading && transactionState.loading

                    emitState()
                }
            }

            sendTransactionService.start(viewModelScope)
            viewModelScope.launch {
                sendTransactionService.setSendTransactionData(SendTransactionData.Evm(transactionData, null))
            }
        }
    }

    suspend fun send() = withContext(Dispatchers.Default) {
        sendTransactionService.sendTransaction()
    }

    @AssistedFactory
    interface Factory {
        fun create(transactionHash: String, optionType: SpeedUpCancelType, blockchainType: BlockchainType): TransactionSpeedUpCancelViewModel
    }
}

data class TransactionSpeedUpCancelUiState(
    val sendTransactionState: SendTransactionServiceState,
    val sectionViewItems: List<SectionViewItem>,
    val error: Throwable?,
    val sendEnabled: Boolean,
    val initialLoading: Boolean
)

class TransactionAlreadyInBlock : Exception()