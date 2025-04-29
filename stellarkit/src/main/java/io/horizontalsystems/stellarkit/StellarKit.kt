package io.horizontalsystems.stellarkit

import android.content.Context
import android.util.Log
import io.horizontalsystems.stellarkit.room.KitDatabase
import io.horizontalsystems.stellarkit.room.Operation
import io.horizontalsystems.stellarkit.room.OperationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
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
    private val keyPair: KeyPair,
    network: Network,
    db: KitDatabase,
) {
    val isMainNet = network == Network.MainNet
    val sendFee: BigDecimal = BigDecimal(Transaction.MIN_BASE_FEE.toBigInteger(), 7)

    private val serverUrl = when (network) {
        Network.MainNet -> "https://horizon.stellar.lobstr.co"
        Network.TestNet -> "https://horizon-testnet.stellar.org"
    }
    private val server = Server(serverUrl)
    private val accountId = keyPair.accountId
    private val balancesManager = BalancesManager(
        server,
        db.balanceDao(),
        accountId
    )

    private val operationManager = OperationManager(server, db.operationDao(), accountId)

    val receiveAddress get() = accountId

    val operationsSyncStateFlow by operationManager::syncStateFlow
    val syncStateFlow by balancesManager::syncStateFlow
    val balanceFlow by balancesManager::xlmBalanceFlow
    val assetBalanceMapFlow by balancesManager::assetBalanceMapFlow

    val balance get() = balanceFlow.value
    val assetBalanceMap get() = assetBalanceMapFlow.value

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        coroutineScope.launch {
//            apiListener.transactionFlow.collect {
//                handleOperation(it)
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

//    private suspend fun handleOperation(operationId: String) {
//        repeat(3) {
//            delay(5000)
//            if (operationManager.isOperationCompleted(operationId)) {
//                return
//            }
//
//            sync()
//        }
//    }

    fun operations(tagQuery: TagQuery, beforeId: Long? = null, limit: Int? = null): List<Operation> {
        return operationManager.operations(tagQuery, beforeId, limit)
    }

    fun operationFlow(tagQuery: TagQuery): Flow<OperationInfo> {
        return operationManager.operationFlow(tagQuery)
    }

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
            async {
                operationManager.sync()
            },
        ).awaitAll()
    }

    fun sendNative(recipient: String, amount: BigDecimal, memo: String?) {
        send(AssetTypeNative(), recipient, amount, memo)
    }

    fun sendAsset(assetId: String, recipient: String, amount: BigDecimal, memo: String?) {
        send(Asset.create(assetId), recipient, amount, memo)
    }

    private fun send(asset: Asset, recipient: String, amount: BigDecimal, memo: String?) {
        if (!keyPair.canSign()) throw WalletError.WatchOnly

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

    sealed class SyncError : Error() {
        data object NotStarted : SyncError() {
            override val message = "Not Started"
        }
    }

    sealed class WalletError : Error() {
        data object WatchOnly : WalletError()
    }

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
            return StellarKit(keyPair, network, db)
        }

        fun validateAddress(address: String) {
            KeyPair.fromAccountId(address)
        }
    }
}
