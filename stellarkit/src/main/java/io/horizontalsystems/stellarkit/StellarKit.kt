package io.horizontalsystems.stellarkit

import android.content.Context
import android.util.Log
import io.horizontalsystems.stellarkit.room.Event
import io.horizontalsystems.stellarkit.room.KitDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.stellar.sdk.Asset
import org.stellar.sdk.AssetTypeNative
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Memo
import org.stellar.sdk.Server
import org.stellar.sdk.Transaction
import org.stellar.sdk.TransactionBuilder
import org.stellar.sdk.operations.PaymentOperation
import java.math.BigDecimal

class StellarKit(
//    private val address: Address,
//    private val apiListener: TonApiListener,
//    private val accountManager: AccountManager,
//    private val jettonManager: JettonManager,
//    private val eventManager: EventManager,
//    private val transactionSender: TransactionSender?,
    val network: Network,
    private val keyPair: KeyPair,
    private val db: KitDatabase,
//    private val transactionSigner: TransactionSigner,
) {
    val sendFee: BigDecimal = BigDecimal(Transaction.MIN_BASE_FEE.toBigInteger(), 7)

    private val serverUrl = when (network) {
        Network.MainNet -> "https://horizon.stellar.lobstr.co"
        Network.TestNet -> "https://horizon-testnet.stellar.org"
    }
    private val server = Server(serverUrl)
    private val accountId = keyPair.accountId
//    private val accountId = "GBXQUJBEDX5TYLJ6D5BGJZFLYF5GZVGXLWA2ZORS5OIA7H6B5O3MHMTP"
    private val balancesManager = BalancesManager(
        server,
        db.balanceDao(),
        accountId
    )

    private val eventManager = EventManager(server, db.operationDao(), accountId)

    val receiveAddress get() = accountId

    val operationsSyncStateFlow by eventManager::syncStateFlow
    val syncStateFlow by balancesManager::syncStateFlow
    val balanceFlow by balancesManager::xlmBalanceFlow
    val assetBalanceMapFlow by balancesManager::assetBalanceMapFlow
//    val jettonSyncStateFlow by jettonManager::syncStateFlow
//    val jettonBalanceMapFlow by jettonManager::jettonBalanceMapFlow
//    val eventSyncStateFlow by eventManager::syncStateFlow

    val balance get() = balanceFlow.value
    val assetBalanceMap get() = assetBalanceMapFlow.value

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        coroutineScope.launch {
//            apiListener.transactionFlow.collect {
//                handleEvent(it)
//            }
        }
    }

    suspend fun refresh() {
        sync()
    }

    suspend fun start() = coroutineScope {
        listOf(
            async {
                sync()
            },
            async {
//                startListener()
            }
        ).awaitAll()
    }

    fun stop() {
//        this.stopListener()
    }

//    private suspend fun handleEvent(eventId: String) {
//        repeat(3) {
//            delay(5000)
//            if (eventManager.isEventCompleted(eventId)) {
//                return
//            }
//
//            sync()
//        }
//    }

    fun operations(tagQuery: TagQuery, beforeId: Long? = null, limit: Int? = null): List<Event> {
        return eventManager.operations(tagQuery, beforeId, limit)
    }

//    fun eventFlow(tagQuery: TagQuery): Flow<EventInfo> {
//        return eventManager.eventFlow(tagQuery)
//    }

//    fun tagTokens(): List<TagToken> {
//        return eventManager.tagTokens()
//    }

//    suspend fun estimateFee(
//        recipient: FriendlyAddress,
//        amount: SendAmount,
//        comment: String?,
//    ): BigInteger {
//        return transactionSender?.estimateFee(recipient, amount, comment)
//            ?: throw WalletError.WatchOnly
//    }

//    suspend fun estimateFee(
//        jettonWallet: Address,
//        recipient: FriendlyAddress,
//        amount: BigInteger,
//        comment: String?,
//    ): BigInteger {
//        return transactionSender?.estimateFee(jettonWallet, recipient, amount, comment)
//            ?: throw WalletError.WatchOnly
//    }

//    fun startListener() {
//        apiListener.start(address = address)
//    }

//    fun stopListener() {
//        apiListener.stop()
//    }

    suspend fun sync() = coroutineScope {
        listOf(
            async {
                balancesManager.sync()
            },
//            async {
//                jettonManager.sync()
//            },
            async {
                eventManager.sync()
            },
        ).awaitAll()
    }

//    suspend fun send(recipient: FriendlyAddress, amount: SendAmount, comment: String?) {
//        transactionSender?.send(recipient, amount, comment) ?: throw WalletError.WatchOnly
//    }

//    suspend fun send(
//        jettonWallet: Address,
//        recipient: FriendlyAddress,
//        amount: BigInteger,
//        comment: String?,
//    ) {
//        transactionSender?.send(jettonWallet, recipient, amount, comment)
//            ?: throw WalletError.WatchOnly
//    }

//    suspend fun send(boc: String) {
//        transactionSender?.send(boc) ?: throw WalletError.WatchOnly
//    }

    fun sendNative(recipient: String, amount: BigDecimal, memo: String?) {
        send(AssetTypeNative(), recipient, amount, memo)
    }

    fun sendAsset(assetId: String, recipient: String, amount: BigDecimal, memo: String?) {
        send(Asset.create(assetId), recipient, amount, memo)
    }

    private fun send(asset: Asset, recipient: String, amount: BigDecimal, memo: String?) {
        val destination = KeyPair.fromAccountId(recipient)

        // First, check to make sure that the destination account exists.
        // You could skip this, but if the account does not exist, you will be charged
        // the transaction fee when the transaction fails.
        // It will throw HttpResponseException if account does not exist or there was another error.
        server.accounts().account(destination.accountId)

        val sourceAccount = server.accounts().account(accountId)

        val paymentOperation = PaymentOperation.builder()
            .destination(destination.accountId)
            .asset(asset)
            .amount(amount)
            .build()

        val transactionBuilder =
            TransactionBuilder(sourceAccount, Network.MainNet.toStellarNetwork())
                .addOperation(paymentOperation)
                .setTimeout(180)
                .setBaseFee(Transaction.MIN_BASE_FEE)

        memo?.let {
            transactionBuilder.addMemo(Memo.text(memo))
        }

        val transaction = transactionBuilder.build()
        transaction.sign(keyPair)

        try {
            val response = server.submitTransaction(transaction)
            Log.e("AAA", "Success! $response")
        } catch (e: Exception) {
            Log.e("AAA", "Something went wrong!", e)
            throw e
        }
    }

//    suspend fun sign(request: SendRequestEntity, tonWallet: TonWallet): String {
//        check(tonWallet is TonWallet.FullAccess)
//
//        return transactionSigner.sign(request, tonWallet)
//    }

//    suspend fun getDetails(request: SendRequestEntity, tonWallet: TonWallet): Event {
//        check(tonWallet is TonWallet.FullAccess)
//
//        return transactionSigner.getDetails(request, tonWallet)
//    }

    sealed class SyncError : Error() {
        data object NotStarted : SyncError() {
            override val message = "Not Started"
        }
    }

    sealed class WalletError : Error() {
        data object WatchOnly : WalletError()
    }

//    enum SendAmount {
//        case amount(value: BigUInt)
//        case max
//    }

    companion object {
        fun getInstance(
            stellarWallet: StellarWallet,
            network: Network,
            context: Context,
            walletId: String,
        ): StellarKit {
            val keyPair = when (stellarWallet) {
                is StellarWallet.Seed -> {
                    KeyPair.fromBip39Seed(stellarWallet.seed, 0)
                }

                is StellarWallet.WatchOnly -> {
                    KeyPair.fromAccountId(stellarWallet.addressStr)
                }
            }

            val db = KitDatabase.getInstance(context, "stellar-${walletId}-${network.name}")
            return StellarKit(network, keyPair, db)
        }

//        fun getTonApi(network: Network) = TonApi(network, okHttpClient)
//        fun getTransactionSigner(api: TonApi) = TransactionSigner(api)

//        suspend fun getJetton(network: Network, address: Address): Jetton {
//            return getTonApi(network).getJettonInfo(address)
//        }

        fun validateAddress(address: String) {
            KeyPair.fromAccountId(address)
        }
    }

//    sealed class SendAmount {
//        data class Amount(val value: BigInteger) : SendAmount()
//        data object Max : SendAmount()
//    }

}