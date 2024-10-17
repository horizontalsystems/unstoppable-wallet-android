package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.lifecycle.viewModelScope
import com.tonapps.wallet.data.core.entity.SendRequestEntity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.TonTransactionRecord
import io.horizontalsystems.bankwallet.core.managers.TonConnectManager
import io.horizontalsystems.bankwallet.core.managers.TonKitWrapper
import io.horizontalsystems.bankwallet.core.managers.toTonWalletFullAccess
import io.horizontalsystems.bankwallet.core.meta
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.core.TonWallet
import io.horizontalsystems.tonkit.models.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TonConnectSendRequestViewModel(
    private val sendRequestEntity: SendRequestEntity?,
    private val accountManager: IAccountManager,
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
