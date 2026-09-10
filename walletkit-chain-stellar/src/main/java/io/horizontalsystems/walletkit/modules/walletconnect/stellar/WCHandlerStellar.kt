package io.horizontalsystems.walletkit.modules.walletconnect.stellar

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.managers.StellarKitManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.modules.walletconnect.handler.IWCHandler
import io.horizontalsystems.walletkit.modules.walletconnect.handler.MethodData
import io.horizontalsystems.walletkit.modules.walletconnect.handler.UnsupportedMethodException
import io.horizontalsystems.walletkit.modules.walletconnect.request.AbstractWCAction
import io.horizontalsystems.dapp.core.HSDAppRequest
import io.horizontalsystems.stellarkit.StellarKit

class WCHandlerStellar(private val stellarKitManager: StellarKitManager) : IWCHandler {
    override val chainNamespace = "stellar"

    override val supportedChains = listOf("stellar:pubnet")
    override val supportedMethods = listOf("stellar_signAndSubmitXDR", "stellar_signXDR")
    override val supportedEvents = listOf<String>()

    override fun getAction(request: HSDAppRequest, chainInternalId: String?): AbstractWCAction {
        val account = App.accountManager.activeAccount!!
        val stellarKit = getStellarKit(account)
        val walletAddress = activeStellarAddress(account)

        return when (request.method) {
            "stellar_signAndSubmitXDR" -> WCActionStellarSignAndSubmitXdr(
                request.params,
                stellarKit,
                walletAddress,
            )

            "stellar_signXDR" -> WCActionStellarSignXdr(
                request.params,
                request.peerMetaData?.name ?: "",
                stellarKit,
                walletAddress,
            )

            else -> throw UnsupportedMethodException(request.method)
        }
    }

    // Address of the active account, so the sign screen can flag a transaction (or operation)
    // sourced from some other account. Best-effort: null skips the check rather than failing.
    private fun activeStellarAddress(account: Account): String? = try {
        stellarKitManager.getAddress(account.type)
    } catch (e: Exception) {
        null
    }

    private fun getStellarKit(account: Account): StellarKit {
        return stellarKitManager.getStellarKitWrapper(account).stellarKit
    }

    override fun getAccountAddresses(account: Account): List<String> {
        val address = stellarKitManager.getAddress(account.type)
        return supportedChains.map { "$it:$address" }
    }

    override fun getMethodData(method: String, chainInternalId: String?): MethodData {
        val title = when (method) {
            "stellar_signAndSubmitXDR" -> "Approve Transaction"
            "stellar_signXDR" -> "Sign Request"
            else -> method
        }

        val shortTitle = when (method) {
            "stellar_signAndSubmitXDR" -> "Sign"
            "stellar_signXDR" -> "Sign"
            else -> method
        }

        return MethodData(title, shortTitle, "Stellar")
    }

    override fun getChainName(chainInternalId: String) = "Stellar"
}
