package cash.p.terminal.modules.tonconnect

import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.TonConnectManager
import cash.p.terminal.core.managers.TonKitWrapper
import cash.p.terminal.core.managers.toTonWalletFullAccess
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.meta
import cash.p.terminal.wallet.transaction.TransactionSource
import com.tonapps.blockchain.ton.TonNetwork
import com.tonapps.extensions.equalsAddress
import com.tonapps.wallet.data.core.entity.RawMessageEntity
import com.tonapps.wallet.data.core.entity.SendRequestEntity
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.tonkit.core.TonWallet
import io.horizontalsystems.tonkit.models.Event
import io.horizontalsystems.tonkit.models.SignTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TonConnectSendRequestViewModel(
    private val signTransaction: SignTransaction?,
    private val accountManager: cash.p.terminal.wallet.IAccountManager,
    private val tonConnectManager: TonConnectManager,
) : ViewModelUiState<TonConnectSendRequestUiState>() {
    private val sendRequestEntity = signTransaction?.request

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

    private fun isInvalidNetwork(networkId: Int): Boolean {
        return !(networkId == TonNetwork.MAINNET.value ||
                networkId == TonNetwork.TESTNET.value)
    }

    private suspend fun TonConnectSendRequestViewModel.responseBadRequest(entity: SendRequestEntity) =
        withContext(Dispatchers.IO) {
            tonConnectKit.badRequest(entity)
        }

    /**
     * Validates that the 'from' address in the request matches the connected account address.
     * @return null if valid, or an error if invalid
     */
    private fun validateFromAddress(
        sendRequestEntity: SendRequestEntity,
        connectionAccountId: String?
    ): TonConnectSendRequestError? {
        val requestAccountId = try {
            sendRequestEntity.fromAccountId
        } catch (e: Exception) {
            return TonConnectSendRequestError.InvalidData("Invalid \"from\" address")
        }

        if (requestAccountId != null && connectionAccountId != null
            && !requestAccountId.equalsAddress(connectionAccountId)
        ) {
            return TonConnectSendRequestError.InvalidData(
                "Invalid \"from\" address. Specified wallet address not connected to this app."
            )
        }

        return null
    }

    private fun validUntilIsInvalid(sendRequestEntity: SendRequestEntity): Boolean {
        return try {
            sendRequestEntity.validUntil
            false
        } catch (e: IllegalArgumentException) {
            true
        }
    }

    private fun requestExpired(sendRequestEntity: SendRequestEntity): Boolean {
        return sendRequestEntity.validUntil < System.currentTimeMillis() / 1000
    }

    private fun addressIsRaw(messages: List<RawMessageEntity>): Boolean {
        messages.forEach { message ->
            if (message.addressValue.contains(":", ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun isTestnet(sendRequestEntity: SendRequestEntity): Boolean {
        return sendRequestEntity.network == TonNetwork.TESTNET
    }

    private suspend fun prepareEnv() {
        if (sendRequestEntity == null) {
            error = TonConnectSendRequestError.EmptySendRequest()
            return
        }

        if (isInvalidNetwork(sendRequestEntity.network.value)) {
            error = TonConnectSendRequestError.InvalidData("Invalid network")
            responseBadRequest(sendRequestEntity)
            return
        }

        validateFromAddress(
            sendRequestEntity,
            signTransaction?.dApp?.accountId
        )?.let { validationError ->
            error = validationError
            responseBadRequest(sendRequestEntity)
            return
        }

        if (validUntilIsInvalid(sendRequestEntity)) {
            error = TonConnectSendRequestError.InvalidData("Invalid validUntil field")

            responseBadRequest(sendRequestEntity)
            return
        }

        if (requestExpired(sendRequestEntity)) {
            error = TonConnectSendRequestError.InvalidData("Field validUntil has expired")

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
        } catch (e: Exception) {
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
        } else if (account != accountManager.activeAccount) {
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
        if (accountBalance != null) {
            val totalSentAmount = sendRequestEntity.messages.sumOf { it.amount }
            if (totalSentAmount > accountBalance) {
                error =
                    TonConnectSendRequestError.InvalidData("Transaction amount exceeds available balance")
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
    /*
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
    */

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
    val currency: Currency,
    val error: TonConnectSendRequestError?,
    val confirmEnabled: Boolean,
    val rejectEnabled: Boolean,
)
