package io.horizontalsystems.solanakit.transactions

import com.solana.actions.Action
import com.solana.actions.findSPLTokenDestinationAddress
import com.solana.actions.serializeAndSendWithFee
import com.solana.api.Api
import com.solana.api.MultipleAccountsRequest
import com.solana.core.Account
import com.solana.core.PublicKey
import com.solana.core.Transaction
import com.solana.core.TransactionInstruction
import com.solana.models.RpcSendTransactionConfig
import com.solana.networking.serialization.serializers.base64.BorshAsBase64JsonArraySerializer
import com.solana.networking.serialization.serializers.solana.AnchorAccountSerializer
import com.solana.networking.serialization.serializers.solana.SolanaResponseSerializer
import com.solana.programs.AssociatedTokenProgram
import com.solana.programs.SystemProgram
import com.solana.programs.TokenProgram
import com.solana.vendor.ContResult
import com.solana.vendor.ResultError
import com.solana.vendor.flatMap
import io.horizontalsystems.solanakit.models.AccountInfoFixed
import io.horizontalsystems.solanakit.network.makeRequestResultWithRepeat
import io.reactivex.Single
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import java.util.Base64

fun <T> Api.getMultipleAccountsFixed(
    serializer: KSerializer<T>,
    accounts: List<PublicKey>,
): Single<List<AccountInfoFixed<T>?>> = Single.create { emitter ->
    getMultipleAccounts(serializer, accounts) { result ->
        result.onSuccess { emitter.onSuccess(it) }
        result.onFailure { emitter.onError(it) }
    }
}

fun <A> Api.getMultipleAccounts(
    serializer: KSerializer<A>,
    accounts: List<PublicKey>,
    onComplete: ((Result<List<AccountInfoFixed<A>?>>) -> Unit)
) {
    CoroutineScope(dispatcher + CoroutineExceptionHandler { coroutineContext, throwable ->
        onComplete(Result.failure(ResultError(throwable)))
    }).launch {
        getMultipleAccountsInfo(
            serializer = serializer,
            accounts
        ).onSuccess {
            val buffers = it.map { account ->
                account
            }
            onComplete(Result.success(buffers))
        }.onFailure {
            onComplete(Result.failure(ResultError(it)))
        }
    }
}

@Serializable
data class MintTokenAccountValue(
    val data: MintTokenAccountInfo?,
    val owner: String,
)

@Serializable
data class MintTokenAccountInfo(
    val parsed: MintTokenAccountInfoParsedData,
    val program: String,
    val space: Int? = null
)

@Serializable
data class MintTokenAccountInfoParsedData(
    val info: MintTokenAccountTokenInfo,
    val type: String
)

@Serializable
data class MintTokenAccountTokenInfo(
    val decimals: Int,
    val isInitialized: Boolean,
    val mintAuthority: String?,
    val supply: String,
)

suspend fun Api.getMultipleMintAccountsInfo(
    accounts: List<PublicKey>,
    encoding: RpcSendTransactionConfig.Encoding = RpcSendTransactionConfig.Encoding.jsonParsed,
    commitment: String = "max",
    length: Int? = null,
    offset: Int? = length?.let { 0 }
): Result<List<MintTokenAccountValue>?> =
    router.makeRequestResultWithRepeat(
        request = MultipleAccountsRequest(
            accounts = accounts.map { it.toBase58() },
            encoding = encoding,
            commitment = commitment,
            length = length,
            offset = offset
        ),
        serializer = SolanaResponseSerializer(ListSerializer(MintTokenAccountValue.serializer()))
    ).let { result ->
        @Suppress("UNCHECKED_CAST")
        if (result.isSuccess && result.getOrNull() == null) Result.success(null)
        else result as Result<List<MintTokenAccountValue>> // safe cast, null case handled above
    }

suspend fun <A> Api.getMultipleAccountsInfo(
    serializer: KSerializer<A>,
    accounts: List<PublicKey>,
    encoding: RpcSendTransactionConfig.Encoding = RpcSendTransactionConfig.Encoding.base64,
    commitment: String = "max",
    length: Int? = null,
    offset: Int? = length?.let { 0 }
): Result<List<AccountInfoFixed<A>?>> =
    router.makeRequestResultWithRepeat(
        request = MultipleAccountsRequest(
            accounts = accounts.map { it.toBase58() },
            encoding = encoding,
            commitment = commitment,
            length = length,
            offset = offset
        ),
        serializer = MultipleAccountsSerializer(serializer)
    ).let { result ->
        @Suppress("UNCHECKED_CAST")
        if (result.isSuccess && result.getOrNull() == null) Result.success(listOf())
        else result as Result<List<AccountInfoFixed<A>?>> // safe cast, null case handled above
    }

internal fun <A> MultipleAccountsSerializer(serializer: KSerializer<A>) =
    MultipleAccountsInfoSerializer(
        BorshAsBase64JsonArraySerializer(
            AnchorAccountSerializer(serializer.descriptor.serialName, serializer)
        )
    )

private fun <D> MultipleAccountsInfoSerializer(serializer: KSerializer<D>) =
    SolanaResponseSerializer(ListSerializer(AccountInfoFixed.serializer(serializer).nullable))

fun Action.sendSOL(
    account: Account,
    destination: PublicKey,
    amount: Long,
    instructions: List<TransactionInstruction>,
    recentBlockHash: String
) = Single.create { emitter ->
    val transferInstruction = SystemProgram.transfer(account.publicKey, destination, amount)
    val transaction = Transaction()

    if (instructions.isNotEmpty()) {
        transaction.add(*instructions.toTypedArray())
    }

    transaction.add(transferInstruction)

    this.serializeAndSendWithFee(transaction, listOf(account), recentBlockHash) { result ->
        result.onSuccess {
            emitter.onSuccess(Pair(it, encodeBase64(transaction)))
        }.onFailure {
            emitter.onError(it)
        }
    }
}

fun Action.sendSPLTokens(
    mintAddress: PublicKey,
    fromPublicKey: PublicKey,
    destinationAddress: PublicKey,
    amount: Long,
    allowUnfundedRecipient: Boolean = false,
    account: Account,
    instructions: List<TransactionInstruction>,
    recentBlockHash: String
) = Single.create { emitter ->
    ContResult { cb ->
        this.findSPLTokenDestinationAddress(
            mintAddress,
            destinationAddress,
            allowUnfundedRecipient
        ) { cb(it) }
    }.flatMap { spl ->
        val toPublicKey = spl.first
        val unregisteredAssociatedToken = spl.second
        if (fromPublicKey.toBase58() == toPublicKey.toBase58()) {
            return@flatMap ContResult.failure(ResultError("Same send and destination address."))
        }
        val transaction = Transaction()

        if (instructions.isNotEmpty()) {
            transaction.add(*instructions.toTypedArray())
        }

        // create associated token address
        if (unregisteredAssociatedToken) {
            val mint = mintAddress
            val owner = destinationAddress
            val createATokenInstruction =
                AssociatedTokenProgram.createAssociatedTokenAccountInstruction(
                    mint = mint,
                    associatedAccount = toPublicKey,
                    owner = owner,
                    payer = account.publicKey
                )
            transaction.add(createATokenInstruction)
        }

        // send instruction
        val sendInstruction =
            TokenProgram.transfer(fromPublicKey, toPublicKey, amount, account.publicKey)
        transaction.add(sendInstruction)
        return@flatMap ContResult.success(transaction)
    }.flatMap { transaction ->
        return@flatMap ContResult<Pair<String, String>, ResultError> { cb ->
            this.serializeAndSendWithFee(transaction, listOf(account), recentBlockHash) { result ->
                result.onSuccess {
                    cb(com.solana.vendor.Result.success(Pair(it, encodeBase64(transaction))))
                }.onFailure {
                    cb(com.solana.vendor.Result.failure(ResultError(it)))
                }
            }
        }
    }.run { result ->
        result.onSuccess {
            emitter.onSuccess(it)
        }.onFailure {
            emitter.onError(it)
        }
    }
}

private fun encodeBase64(transaction: Transaction): String {
    val serialized = transaction.serialize()
    return Base64.getEncoder().encodeToString(serialized)
}
