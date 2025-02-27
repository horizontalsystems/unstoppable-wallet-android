package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.lifecycle.viewModelScope
import com.tonapps.blockchain.ton.TonNetwork
import com.tonapps.extensions.equalsAddress
import com.tonapps.wallet.data.core.entity.RawMessageEntity
import com.tonapps.wallet.data.core.entity.SendRequestEntity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.TonTransactionRecord
import io.horizontalsystems.bankwallet.core.managers.TonConnectManager
import io.horizontalsystems.bankwallet.core.managers.TonKitWrapper
import io.horizontalsystems.bankwallet.core.managers.toTonWalletFullAccess
import io.horizontalsystems.bankwallet.core.meta
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.core.TonWallet
import io.horizontalsystems.tonkit.models.Event
import io.horizontalsystems.tonkit.models.SignTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TonConnectSendRequestViewModel(
    private val signTransaction: SignTransaction?,
    private val accountManager: IAccountManager,
    private val tonConnectManager: TonConnectManager,
) : ViewModelUiState<TonConnectSendRequestUiState>() {

    private val sendRequestEntity = signTransaction?.request
    private var error: TonConnectSendRequestError? = null
    private val transactionSigner = tonConnectManager.transactionSigner
    private val tonConnectKit = App.tonConnectManager.kit
    private var tonTransactionRecord: TonTransactionRecord? = null

    private var tonWallet: TonWallet.FullAccess? = null
    private var tonKitWrapper: TonKitWrapper? = null
    private var tonEvent: Event? = null

    override fun createState() = TonConnectSendRequestUiState(
        tonTransactionRecord = tonTransactionRecord,
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

        val connectionAccountId = signTransaction?.dApp?.accountId
        val requestAccountId = sendRequestEntity.fromAccountId

        if (requestAccountId != null && connectionAccountId != null && !requestAccountId.equalsAddress(connectionAccountId)) {
            error = TonConnectSendRequestError.InvalidData("Invalid \"from\" address. Specified wallet address not connected to this app.")

            responseBadRequest(sendRequestEntity)
            return
        }

        if (validUntilIsInvalid(sendRequestEntity)) {
            error = TonConnectSendRequestError.InvalidData("Invalid validUntil field")

            responseBadRequest(sendRequestEntity)
            return
        }

        try {
            val messages = sendRequestEntity.messages
            if (addressIsRaw(messages)) {
                error = TonConnectSendRequestError.InvalidData("Send to Raw address is not allowed")
                responseBadRequest(sendRequestEntity)
                return
            }
        }catch (e: Exception){
            error = TonConnectSendRequestError.InvalidData("Failed to parse messages")
            responseBadRequest(sendRequestEntity)
            return
        }

        if (isTestnet(sendRequestEntity)) {
            error = TonConnectSendRequestError.InvalidData("Send to Testnet is not allowed")
            responseBadRequest(sendRequestEntity)
            return
        }

        if (sendRequestEntity.messages.isEmpty()) {
            error = TonConnectSendRequestError.InvalidData("Empty messages")
            responseBadRequest(sendRequestEntity)
            return
        }

        val (accountId, _) = sendRequestEntity.dAppId.split(":", limit = 2)
        val account = accountManager.account(accountId)

        if (account == null) {
            error = TonConnectSendRequestError.AccountNotFound()
            return
        } else if (account != accountManager.activeAccount){
            error = TonConnectSendRequestError.DifferentAccount("Incorrect account selected")
            responseBadRequest(sendRequestEntity)
            return
        }

        val tonWallet = account.type.toTonWalletFullAccess().also {
            tonWallet = it
        }
        val tonKitWrapper = App.tonKitManager.getNonActiveTonKitWrapper(account).also {
            tonKitWrapper = it
        }

        val accountBalance = tonKitWrapper.tonKit.account?.balance
        if (accountBalance != null){
            val totalSentAmount = sendRequestEntity.messages.sumOf { it.amount }
            if (totalSentAmount > accountBalance){
                error = TonConnectSendRequestError.InvalidData("Transaction amount exceeds available balance")
                responseBadRequest(sendRequestEntity)
                return
            }
        }

        val tonEvent = try {
            val event = transactionSigner.getDetails(sendRequestEntity, tonWallet)
            tonEvent = event
            event
        } catch (e: Exception) {
            error = TonConnectSendRequestError.InvalidData("Failed to get details")
            responseBadRequest(sendRequestEntity)
            return
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

    private fun isTestnet(sendRequestEntity: SendRequestEntity): Boolean {
        return sendRequestEntity.network == TonNetwork.TESTNET
    }

    private fun addressIsRaw(messages: List<RawMessageEntity>): Boolean {
        messages.forEach { message ->
            if (message.addressValue.contains(":", ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private suspend fun TonConnectSendRequestViewModel.responseBadRequest(entity: SendRequestEntity) {
        withContext(Dispatchers.IO) {
            tonConnectKit.badRequest(entity)
        }
    }

    private fun validUntilIsInvalid(sendRequestEntity: SendRequestEntity): Boolean {
        return try {
            val result = sendRequestEntity.validUntil
            if (result == 0L) {
                throw IllegalArgumentException("Invalid validUntil")
            }
            false
        } catch (e: IllegalArgumentException) {
            true
        }
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
    class InvalidData(override val message: String) : TonConnectSendRequestError()
    class EmptySendRequest : TonConnectSendRequestError()
    class AccountNotFound : TonConnectSendRequestError()
    class DifferentAccount(override val message: String) : TonConnectSendRequestError()
    class Other(override val message: String) : TonConnectSendRequestError()
}

data class TonConnectSendRequestUiState(
    val tonTransactionRecord: TonTransactionRecord?,
    val error: TonConnectSendRequestError?,
    val confirmEnabled: Boolean,
    val rejectEnabled: Boolean,
)
