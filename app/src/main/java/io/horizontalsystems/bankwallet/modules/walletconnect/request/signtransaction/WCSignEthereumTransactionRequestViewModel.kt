package io.horizontalsystems.bankwallet.modules.walletconnect.request.signtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.modules.evmfee.GasData
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldNonce
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ValueType
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WalletConnectTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WCSignEthereumTransactionRequestViewModel(
    private val evmKit: EvmKitWrapper,
    baseCoinService: EvmCoinService,
    private val sendEvmTransactionViewItemFactory: SendEvmTransactionViewItemFactory,
    private val dAppName: String,
    transaction: WalletConnectTransaction
) : ViewModelUiState<WCSignEthereumTransactionRequestUiState>() {

    private val transactionData = TransactionData(
        transaction.to,
        transaction.value,
        transaction.data
    )

    private var gasData: GasData? = null
    private var nonce: Long? = null
    private var feeAmountData: SendModule.AmountData?
    private var fields: List<DataField>

    init {
        val gasPrice = if (transaction.maxFeePerGas != null && transaction.maxPriorityFeePerGas != null) {
            GasPrice.Eip1559(transaction.maxFeePerGas, transaction.maxPriorityFeePerGas)
        } else if (transaction.gasPrice != null) {
            GasPrice.Legacy(transaction.gasPrice)
        } else {
            null
        }

        feeAmountData = if (gasPrice != null && transaction.gasLimit != null) {
            GasData(gasLimit = transaction.gasLimit, gasPrice = gasPrice).let {
                gasData = it
                baseCoinService.amountData(
                    it.estimatedFee,
                    it.isSurcharged
                )
            }
        } else {
            null
        }

        nonce = transaction.nonce

        fields = if (transaction.nonce != null) {
            listOf(DataFieldNonce(transaction.nonce))
        } else {
            emptyList()
        }
    }

    override fun createState() = WCSignEthereumTransactionRequestUiState(
        networkFee = feeAmountData,
        cautions = emptyList(),
        transactionFields = fields,
        sectionViewItems = getSectionViewItems()
    )

    private fun getSectionViewItems(): List<SectionViewItem> {
        val items = sendEvmTransactionViewItemFactory.getItems(
            transactionData,
            null,
            evmKit.evmKit.decorate(transactionData)
        ) + SectionViewItem(
            buildList {
                add(
                    ViewItem.Value(
                        Translator.getString(R.string.WalletConnect_SignMessageRequest_dApp),
                        dAppName,
                        ValueType.Regular
                    )
                )
            }
        )

        return items
    }

    suspend fun sign() = withContext(Dispatchers.Default) {
        val signer = evmKit.signer ?: throw WCSessionManager.RequestDataError.NoSigner
        val gasData = gasData ?: throw WCSessionManager.RequestDataError.InvalidGasPrice
        val nonce = nonce ?: throw WCSessionManager.RequestDataError.InvalidNonce

        val signature = signer.signedTransaction(
            address = transactionData.to,
            value = transactionData.value,
            transactionInput = transactionData.input,
            gasPrice = gasData.gasPrice,
            gasLimit = gasData.gasLimit,
            nonce = nonce
        )

        WCDelegate.sessionRequestEvent?.let { sessionRequest ->
            WCDelegate.respondPendingRequest(sessionRequest.request.id, sessionRequest.topic, signature.toHexString())
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
            val token = App.evmBlockchainManager.getBaseToken(blockchainType)!!
            val evmKitWrapper = App.evmBlockchainManager.getEvmKitManager(blockchainType).evmKitWrapper!!
            val coinServiceFactory = EvmCoinServiceFactory(
                token,
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


            return WCSignEthereumTransactionRequestViewModel(
                evmKitWrapper,
                coinServiceFactory.baseCoinService,
                sendEvmTransactionViewItemFactory,
                peerName,
                transaction
            ) as T
        }
    }
}

data class WCSignEthereumTransactionRequestUiState(
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val transactionFields: List<DataField>,
    val sectionViewItems: List<SectionViewItem>
)
