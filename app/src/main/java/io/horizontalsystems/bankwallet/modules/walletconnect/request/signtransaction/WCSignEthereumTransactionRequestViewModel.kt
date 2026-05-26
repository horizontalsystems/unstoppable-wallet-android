package io.horizontalsystems.bankwallet.modules.walletconnect.request.signtransaction

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
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.evmfee.GasData
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldNonce
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WalletConnectTransaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = WCSignEthereumTransactionRequestViewModel.Factory::class)
class WCSignEthereumTransactionRequestViewModel @AssistedInject constructor(
    @Assisted transaction: WalletConnectTransaction,
    @Assisted blockchainType: BlockchainType,
    evmBlockchainManager: EvmBlockchainManager,
    marketKit: MarketKitWrapper,
    currencyManager: CurrencyManager,
    coinManager: ICoinManager,
    evmLabelManager: EvmLabelManager,
    contactsRepository: ContactsRepository,
) : ViewModelUiState<WCSignEthereumTransactionRequestUiState>() {

    private val evmKit: EvmKitWrapper
    private val sendEvmTransactionViewItemFactory: SendEvmTransactionViewItemFactory

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
        val token = evmBlockchainManager.getBaseToken(blockchainType)!!
        evmKit = evmBlockchainManager.getEvmKitManager(blockchainType).evmKitWrapper!!
        val coinServiceFactory = EvmCoinServiceFactory(token, marketKit, currencyManager, coinManager)
        val baseCoinService = coinServiceFactory.baseCoinService
        sendEvmTransactionViewItemFactory = SendEvmTransactionViewItemFactory(
            evmLabelManager, coinServiceFactory, contactsRepository, blockchainType
        )

        val gasPrice = transaction.getGasPriceObj()

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
            WCDelegate.respondPendingRequest(sessionRequest.requestId, sessionRequest.topic, signature.toHexString())
        }
    }

    fun reject() {
        WCDelegate.sessionRequestEvent?.let { sessionRequest ->
            WCDelegate.rejectRequest(sessionRequest.topic, sessionRequest.requestId)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(transaction: WalletConnectTransaction, blockchainType: BlockchainType): WCSignEthereumTransactionRequestViewModel
    }
}

data class WCSignEthereumTransactionRequestUiState(
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val transactionFields: List<DataField>,
    val sectionViewItems: List<SectionViewItem>
)
