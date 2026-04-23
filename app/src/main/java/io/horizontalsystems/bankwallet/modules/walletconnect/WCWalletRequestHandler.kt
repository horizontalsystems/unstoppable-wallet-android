package io.horizontalsystems.bankwallet.modules.walletconnect

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.dapp.core.DAppManager
import io.horizontalsystems.dapp.core.HSDAppRequest
import io.horizontalsystems.ethereumkit.core.hexStringToIntOrNull

class WCWalletRequestHandler(
    private val evmBlockchainManager: EvmBlockchainManager
) {
    private val gson by lazy { Gson() }

    fun handle(request: HSDAppRequest): Boolean {
        try {
            val params = JsonParser.parseString(request.params).asJsonArray
            val chain = gson.fromJson(params.first(), WalletConnectChain::class.java)

            return when (request.method) {
                "wallet_addEthereumChain",
                "wallet_switchEthereumChain" -> {
                    val blockchain = chain.chainId.hexStringToIntOrNull()
                        ?.let { evmBlockchainManager.getBlockchain(it) }

                    if (blockchain != null) {
                        DAppManager.respondRequest(
                            topic = request.topic,
                            requestId = request.requestId,
                            result = "null",
                            onError = { Log.e("WCWalletHandler", "${request.method} response error: $it") },
                        )
                    } else {
                        DAppManager.rejectRequest(
                            topic = request.topic,
                            requestId = request.requestId,
                            onError = { Log.e("WCWalletHandler", "${request.method} reject error: $it") },
                        )
                    }
                    true
                }

                else -> false
            }
        } catch (error: Throwable) {
            return false
        }
    }

    data class WalletConnectChain(
        val chainId: String,
        val chainName: String?,
        val rpcUrls: List<String>?,
        val iconUrls: List<String>?,
        val nativeCurrency: WalletConnectNativeCurrency?,
        val blockExplorerUrls: List<String>?,
    )

    data class WalletConnectNativeCurrency(
        val name: String,
        val symbol: String,
        val decimals: Int,
    )
}
