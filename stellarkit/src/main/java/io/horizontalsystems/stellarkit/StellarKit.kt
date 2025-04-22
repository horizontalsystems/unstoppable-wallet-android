package io.horizontalsystems.stellarkit

import android.content.Context
import android.util.Log
import io.horizontalsystems.stellarkit.room.KitDatabase
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Server

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
    private val balancesManager: BalancesManager

    val receiveAddress: String = this.keyPair.accountId

    init {
        balancesManager = BalancesManager(
            server,
            db.balanceDao(),
//            keyPair.accountId
            "GBXQUJBEDX5TYLJ6D5BGJZFLYF5GZVGXLWA2ZORS5OIA7H6B5O3MHMTP"
        )
    }

    suspend fun start() {
        balancesManager.sync()

        Log.e("AAA", "keyPair.accountId: ${keyPair.accountId}")
//        val stellarWallet = Wallet(StellarConfiguration.Testnet)
//        val stellarWallet = Wallet(StellarConfiguration(org.stellar.sdk.Network.PUBLIC, "https://horizon.stellar.lobstr.co"))

//        stellarWallet.stellar().account().getInfo(keyPair.accountId, server)

//        Log.e("AAA", "Thread: ${Thread.currentThread().name}")

    }

    fun stop() {
//        TODO("Not yet implemented")
    }

    fun refresh() {
//        TODO("Not yet implemented")
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
}

sealed interface StellarWallet {
    data class WatchOnly(val addressStr: String) : StellarWallet
    data class Seed(val seed: ByteArray) : StellarWallet
}

enum class Network {
    MainNet, TestNet;
}
