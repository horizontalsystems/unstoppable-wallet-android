package io.horizontalsystems.walletkit.modules.walletconnect.request.sendtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.walletkit.core.toHexString
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceState
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.send.SendModule
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import io.horizontalsystems.walletkit.modules.walletconnect.WCDelegate
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.toEvmTransactionData

class WCSendEthereumTransactionRequestViewModel(
    private val sendEvmTransactionViewItemFactory: SendEvmTransactionViewItemFactory,
    transaction: WalletConnectTransaction,
    blockchainType: BlockchainType
) : ViewModelUiState<WCSendEthereumTransactionRequestUiState>() {
    val sendTransactionService: SendTransactionServiceEvm

    private val transactionData = TransactionData(
        transaction.to,
        transaction.value,
        transaction.data
    )

    private var sendTransactionState: SendTransactionServiceState

    init {
        sendTransactionService = SendTransactionServiceEvm(
            blockchainType = blockchainType,
            initialGasPrice = transaction.getGasPriceObj(),
            initialNonce = transaction.nonce
        )
        sendTransactionState = sendTransactionService.stateFlow.value

        viewModelScope.launch {
            sendTransactionService.stateFlow.collect { transactionState ->
                sendTransactionState = transactionState
                emitState()
            }
        }

        sendTransactionService.start(viewModelScope)

        viewModelScope.launch {
            sendTransactionService.setSendTransactionData(SendTransactionData.Evm(transactionData.toEvmTransactionData(), null))
        }
    }

    override fun createState() = WCSendEthereumTransactionRequestUiState(
        networkFee = sendTransactionState.networkFee,
        cautions = sendTransactionState.cautions,
        sendEnabled = sendTransactionState.sendable,
        transactionFields = sendTransactionState.fields,
        sectionViewItems = getSectionViewItems()
    )

    // The chain is rendered on the approval screen from the session request, which already
    // resolves it, so no empty section is appended here.
    private fun getSectionViewItems(): List<SectionViewItem> =
        sendEvmTransactionViewItemFactory.getItems(
            transactionData,
            null,
            sendTransactionService.decorate(transactionData)
        )

    // NonCancellable: the caller is a UI-scoped coroutine that dies with the composition (locking
    // the app tears the sheet down mid-flight). sendTransaction() suspends on the network, so a
    // plain cancellation could land after the transaction broadcast but before the dApp response —
    // leaving the request pending and inviting a second send. Once the user has confirmed, run the
    // broadcast-and-respond block to completion.
    suspend fun confirm() = withContext(Dispatchers.Default + NonCancellable) {
        val sendResult = sendTransactionService.sendTransaction()
        val transactionHash = sendResult.transactionHash ?: throw Exception("No transaction hash")

        WCDelegate.sessionRequestEvent?.let { sessionRequest ->
            WCDelegate.respondPendingRequest(sessionRequest.requestId, sessionRequest.topic, transactionHash)
        }
    }

    fun reject(topic: String, requestId: Long) {
        // Clear the active request pointer synchronously (guarded by the displayed request id).
        // rejectRequest() only nulls it in its async onSuccess, but the sheet closes immediately and
        // reEmitPendingWcEventIfNeeded() would otherwise re-open the same request while it's still
        // non-null. Use the displayed request id so a newer active request is not wiped.
        WCDelegate.discardActiveSessionRequest(requestId)
        WCDelegate.rejectRequest(topic, requestId)
    }

    class Factory(
        private val blockchainType: BlockchainType,
        private val transaction: WalletConnectTransaction,
        private val peerName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
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

            return WCSendEthereumTransactionRequestViewModel(
                sendEvmTransactionViewItemFactory,
                transaction,
                blockchainType
            ) as T
        }
    }
}

data class WCSendEthereumTransactionRequestUiState(
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val sendEnabled: Boolean,
    val transactionFields: List<DataField>,
    val sectionViewItems: List<SectionViewItem>
)
