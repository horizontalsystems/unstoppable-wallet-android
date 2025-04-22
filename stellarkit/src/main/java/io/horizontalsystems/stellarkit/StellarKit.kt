package io.horizontalsystems.stellarkit

import android.content.Context
import io.horizontalsystems.stellarkit.room.KitDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Server
import java.math.BigDecimal

class StellarKit(
    val network: Network,
    private val keyPair: KeyPair,
    private val db: KitDatabase,
) {
    private val serverUrl = when (network) {
        Network.MainNet -> "https://horizon.stellar.lobstr.co"
        Network.TestNet -> "https://horizon-testnet.stellar.org"
    }
    private val server = Server(serverUrl)
//    private val accountId = keyPair.accountId
    private val accountId = "GBXQUJBEDX5TYLJ6D5BGJZFLYF5GZVGXLWA2ZORS5OIA7H6B5O3MHMTP"
    private val balancesManager = BalancesManager(
        server,
        db.balanceDao(),
        accountId
    )

    private val operationManager = OperationManager(server, accountId)

    val receiveAddress: String = this.keyPair.accountId

    val syncStateFlow by balancesManager::syncStateFlow
    val balanceFlow: StateFlow<BigDecimal> by balancesManager::xlmBalanceFlow
    val balance: BigDecimal get() = balanceFlow.value

    suspend fun start() = coroutineScope {
        listOf(
            async {
                sync()
            },
//            async {
//                startListener()
//            }
        ).awaitAll()
    }


    suspend fun sync() = coroutineScope {
        listOf(
            async {
                balancesManager.sync()
            },
//            async {
//                jettonManager.sync()
//            },
            async {
                operationManager.sync()
            },
        ).awaitAll()
    }


    fun stop() {
//                this.stopListener()
//        TODO("Not yet implemented")
    }

    suspend fun refresh() {
        sync()
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
            return StellarKit(network, keyPair, db)
        }
    }

    sealed class SyncError : Error() {
        data object NotStarted : SyncError() {
            override val message = "Not Started"
        }
    }
}

sealed interface StellarWallet {
    data class WatchOnly(val addressStr: String) : StellarWallet
    data class Seed(val seed: ByteArray) : StellarWallet
}

enum class Network {
    MainNet, TestNet;
}
