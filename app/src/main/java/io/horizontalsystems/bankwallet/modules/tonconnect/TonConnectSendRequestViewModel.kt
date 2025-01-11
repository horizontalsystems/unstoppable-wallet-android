package cash.p.terminal.modules.tonconnect

import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.adapters.TonTransactionRecord
import cash.p.terminal.core.managers.TonConnectManager
import cash.p.terminal.core.managers.TonKitWrapper
import cash.p.terminal.core.managers.toTonWalletFullAccess
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.meta
import cash.p.terminal.wallet.transaction.TransactionSource
import com.tonapps.wallet.data.core.entity.SendRequestEntity
import io.horizontalsystems.tonkit.core.TonWallet
import io.horizontalsystems.tonkit.models.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TonConnectSendRequestViewModel(
    private val sendRequestEntity: SendRequestEntity?,
    private val accountManager: cash.p.terminal.wallet.IAccountManager,
    private val tonConnectManager: TonConnectManager,
) : ViewModelUiState<TonConnectSendRequestUiState>() {

    private var error: TonConnectSendRequestError? = null
    private val transactionSigner = tonConnectManager.transactionSigner
    private val tonConnectKit = App.tonConnectManager.kit
    private var tonTransactionRecord: TonTransactionRecord? = null
    private var currency = App.currencyManager.baseCurrency

    private var tonWallet: TonWallet.FullAccess? = null
    private var tonKitWrapper: TonKitWrapper? = null
    private var tonEvent: Event? = null

    override fun createState() = TonConnectSendRequestUiState(
        tonTransactionRecord = tonTransactionRecord,
        currency = currency,
        error = error,
        confirmEnabled = sendRequestEntity != null && tonWallet != null,
        rejectEnabled = sendRequestEntity != null
    )

    init {
        viewModelScope.launch(Dispatchers.Default) {
            prepareEnv()
            emitState()
        }
    }

    private suspend fun prepareEnv() {
        if (sendRequestEntity == null) {
            error = TonConnectSendRequestError.EmptySendRequest()
            return
        }

        val (accountId, _) = sendRequestEntity.dAppId.split(":", limit = 2)
        val account = accountManager.account(accountId)

        if (account == null) {
            error = TonConnectSendRequestError.AccountNotFound()
            return
        }

        val tonWallet = account.type.toTonWalletFullAccess().also {
            tonWallet = it
        }
        val tonKitWrapper = App.tonKitManager.getNonActiveTonKitWrapper(account).also {
            tonKitWrapper = it
        }

        val tonEvent = transactionSigner.getDetails(sendRequestEntity, tonWallet).also {
            tonEvent = it
        }

        val token = App.coinManager.getToken(TokenQuery(BlockchainType.Ton, TokenType.Native))
        if (token == null) {
            error = TonConnectSendRequestError.Other("Token Ton not found")
            return
        }

        val transactionSource = TransactionSource(token.blockchain, account, token.type.meta)

        val tonTransactionConverter = tonConnectManager.adapterFactory.tonTransactionConverter(
            tonKitWrapper.tonKit.receiveAddress,
            transactionSource
        )

        tonTransactionRecord = tonTransactionConverter?.createTransactionRecord(tonEvent)
    }

    fun confirm() {
        val sendRequestEntity = sendRequestEntity ?: return
        val tonWallet = tonWallet ?: return

        viewModelScope.launch(Dispatchers.Default) {
            val boc = transactionSigner.sign(sendRequestEntity, tonWallet)

            tonKitWrapper?.tonKit?.send(boc)
            tonConnectKit.approve(sendRequestEntity, boc)
        }
    }

    fun reject() {
        val sendRequestEntity = sendRequestEntity ?: return

        viewModelScope.launch(Dispatchers.Default) {
            tonConnectKit.reject(sendRequestEntity)
        }
    }
}

sealed class TonConnectSendRequestError : Error() {
    class EmptySendRequest : TonConnectSendRequestError()
    class AccountNotFound : TonConnectSendRequestError()
    class Other(override val message: String) : TonConnectSendRequestError()
}

data class TonConnectSendRequestUiState(
    val tonTransactionRecord: TonTransactionRecord?,
    val currency: Currency,
    val error: TonConnectSendRequestError?,
    val confirmEnabled: Boolean,
    val rejectEnabled: Boolean,
)
