package com.quantum.wallet.bankwallet.modules.walletconnect.request.sendtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.bankwallet.core.ethereum.CautionViewItem
import com.quantum.wallet.bankwallet.core.ethereum.EvmCoinServiceFactory
import com.quantum.wallet.bankwallet.core.toHexString
import com.quantum.wallet.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import com.quantum.wallet.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import com.quantum.wallet.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceState
import com.quantum.wallet.bankwallet.modules.multiswap.ui.DataField
import com.quantum.wallet.bankwallet.modules.send.SendModule
import com.quantum.wallet.bankwallet.modules.sendevmtransaction.SectionViewItem
import com.quantum.wallet.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import com.quantum.wallet.bankwallet.modules.sendevmtransaction.ValueType
import com.quantum.wallet.bankwallet.modules.sendevmtransaction.ViewItem
import com.quantum.wallet.bankwallet.modules.walletconnect.WCDelegate
import com.quantum.wallet.bankwallet.modules.walletconnect.request.WCChainData
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            sendTransactionService.setSendTransactionData(SendTransactionData.Evm(transactionData, null))
        }
    }

    override fun createState() = WCSendEthereumTransactionRequestUiState(
        networkFee = sendTransactionState.networkFee,
        cautions = sendTransactionState.cautions,
        sendEnabled = sendTransactionState.sendable,
        transactionFields = sendTransactionState.fields,
        sectionViewItems = getSectionViewItems()
    )

    private fun getSectionViewItems(): List<SectionViewItem> {
        val items = sendEvmTransactionViewItemFactory.getItems(
            transactionData,
            null,
            sendTransactionService.decorate(transactionData)
        ) + SectionViewItem(
            buildList {
                val chain: WCChainData? = null // todo: need to implement it
                chain?.let {
                    add(
                        ViewItem.Value(
                            it.name,
                            it.address ?: "",
                            ValueType.Regular
                        )
                    )
                }
            }
        )

        return items
    }

    suspend fun confirm() = withContext(Dispatchers.Default) {
        val sendResult = sendTransactionService.sendTransaction()
        val transactionHash = sendResult.fullTransaction.transaction.hash

        WCDelegate.sessionRequestEvent?.let { sessionRequest ->
            WCDelegate.respondPendingRequest(sessionRequest.request.id, sessionRequest.topic, transactionHash.toHexString())
        }
    }

    fun reject() {
        WCDelegate.sessionRequestEvent?.let { sessionRequest ->
            WCDelegate.rejectRequest(sessionRequest.topic, sessionRequest.request.id)
        }
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
