package io.horizontalsystems.solanakit.core

import android.util.Log
import com.solana.api.Api
import com.solana.core.PublicKey
import com.solana.models.buffer.AccountInfoData
import getTokenAccountBalanceWithRepeat
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.database.main.MainStorage
import io.horizontalsystems.solanakit.database.transaction.TransactionStorage
import io.horizontalsystems.solanakit.models.AccountInfoFixed
import io.horizontalsystems.solanakit.models.FullTokenAccount
import io.horizontalsystems.solanakit.models.MintAccount
import io.horizontalsystems.solanakit.models.TokenAccount
import io.horizontalsystems.solanakit.transactions.SolanaFmService
import io.horizontalsystems.solanakit.transactions.getMultipleAccountsFixed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

interface ITokenAccountListener {
    fun onUpdateTokenSyncState(value: SolanaKit.SyncState)
}

class TokenAccountManager(
    private val walletAddress: String,
    private val rpcClient: Api,
    private val storage: TransactionStorage,
    private val mainStorage: MainStorage,
    private val solanaFmService: SolanaFmService
) {

    var syncState: SolanaKit.SyncState =
        SolanaKit.SyncState.NotSynced(SolanaKit.SyncError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                listener?.onUpdateTokenSyncState(value)
            }
        }

    var listener: ITokenAccountListener? = null

    private val _newTokenAccountsFlow = MutableStateFlow<List<FullTokenAccount>>(listOf())
    val newTokenAccountsFlow: StateFlow<List<FullTokenAccount>> = _newTokenAccountsFlow

    private val _tokenAccountsUpdatedFlow = MutableStateFlow<List<FullTokenAccount>>(listOf())
    val tokenAccountsFlow: StateFlow<List<FullTokenAccount>> = _tokenAccountsUpdatedFlow

    fun tokenBalanceFlow(mintAddress: String): Flow<FullTokenAccount> = _tokenAccountsUpdatedFlow
        .map { tokenAccounts ->
            tokenAccounts.firstOrNull {
                it.mintAccount.address == mintAddress
            }
        }
        .filterNotNull()

    fun fullTokenAccount(mintAddress: String): FullTokenAccount? =
        storage.getFullTokenAccount(mintAddress)

    fun stop(error: Throwable? = null) {
        syncState = SolanaKit.SyncState.NotSynced(error ?: SolanaKit.SyncError.NotStarted())
    }

    @Throws(Exception::class)
    private suspend fun fetchTokenAccounts(walletAddress: String) {
        val tokenAccounts = solanaFmService.tokenAccounts(walletAddress)
        val mintAccounts = tokenAccounts.map { MintAccount(it.mintAddress, it.decimals) }

        storage.saveTokenAccounts(tokenAccounts)
        storage.saveMintAccounts(mintAccounts)
    }

    suspend fun sync(tokenAccounts: List<TokenAccount>? = null) {
        syncState = SolanaKit.SyncState.Syncing()

        var initialSync = mainStorage.isInitialSync()
        if (initialSync) {
            try {
                fetchTokenAccounts(walletAddress)
            } catch (e: Throwable) {
                initialSync = false
                Log.e("TokenAccountManager", "fetchTokenAccounts error: ", e)
            }
        }

        val tokenAccounts = tokenAccounts ?: storage.getTokenAccounts()
        if (tokenAccounts.isEmpty()) return

        val publicKeys = tokenAccounts.map { PublicKey.valueOf(it.address) }
        try {
            val result = rpcClient.getMultipleAccountsFixed(
                serializer = AccountInfoData.serializer(),
                accounts = publicKeys
            ).await()
            handleBalance(tokenAccounts, result, initialSync)
        } catch (error: Throwable) {
            syncState = SolanaKit.SyncState.NotSynced(error)
        }

        if (initialSync) {
            mainStorage.saveInitialSync()
        }
    }

    suspend fun addAccount(
        receivedTokenAccounts: List<TokenAccount>,
        existingMintAddresses: List<String>
    ) {
        storage.saveTokenAccounts(receivedTokenAccounts)

        val tokenAccountUpdated: List<TokenAccount> =
            storage.getTokenAccounts(existingMintAddresses) + receivedTokenAccounts
        sync(tokenAccountUpdated.toSet().toList())
        handleNewTokenAccounts(receivedTokenAccounts)
    }

    fun getFullTokenAccountByMintAddress(mintAddress: String): FullTokenAccount? =
        storage.getFullTokenAccount(mintAddress)

    fun tokenAccounts(): List<FullTokenAccount> =
        storage.getFullTokenAccounts()

    private suspend fun handleBalance(
        tokenAccounts: List<TokenAccount>,
        tokenAccountsBufferInfo: List<AccountInfoFixed<AccountInfoData>?>,
        initialSync: Boolean
    ) {
        val updatedTokenAccounts = mutableListOf<TokenAccount>()

        for ((index, tokenAccount) in tokenAccounts.withIndex()) {
            tokenAccountsBufferInfo[index]?.let { account ->
                val balance = rpcClient.getTokenAccountBalanceWithRepeat(PublicKey.valueOf(tokenAccount.address))
                        .getOrNull()?.amount ?: "0"
                updatedTokenAccounts.add(
                    TokenAccount(
                        address = tokenAccount.address,
                        mintAddress = tokenAccount.mintAddress,
                        balance = balance.toBigDecimal(),
                        decimals = tokenAccount.decimals
                    )
                )
            }
        }

        storage.saveTokenAccounts(updatedTokenAccounts)
        _tokenAccountsUpdatedFlow.tryEmit(storage.getFullTokenAccounts())
        syncState = SolanaKit.SyncState.Synced()
        if (initialSync) {
            handleNewTokenAccounts(updatedTokenAccounts)
        }
    }

    private fun handleNewTokenAccounts(tokenAccounts: List<TokenAccount>) {
        val newFullTokenAccounts = mutableListOf<FullTokenAccount>()
        tokenAccounts.forEach { tokenAccount ->
            storage.getFullTokenAccount(tokenAccount.mintAddress)?.let {
                newFullTokenAccounts.add(it)
            }
        }

        _newTokenAccountsFlow.tryEmit(newFullTokenAccounts)
    }

    fun addTokenAccount(walletAddress: String, mintAddress: String, decimals: Int) {
        if (!storage.tokenAccountExists(mintAddress)) {
            val userTokenMintAddress = associatedTokenAddress(walletAddress, mintAddress)
            val tokenAccount = TokenAccount(
                address = userTokenMintAddress,
                mintAddress = mintAddress,
                balance = BigDecimal.ZERO,
                decimals = decimals
            )
            val mintAccount = MintAccount(mintAddress, decimals)
            storage.addTokenAccount(tokenAccount)
            storage.addMintAccount(mintAccount)
        }
    }

    private fun associatedTokenAddress(
        walletAddress: String,
        tokenMintAddress: String
    ): String {
        return PublicKey.associatedTokenAddress(
            walletAddress = PublicKey(walletAddress),
            tokenMintAddress = PublicKey(tokenMintAddress)
        ).address.toBase58()
    }

}
