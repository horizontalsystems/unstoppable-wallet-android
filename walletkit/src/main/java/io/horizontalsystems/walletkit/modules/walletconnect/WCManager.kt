package io.horizontalsystems.walletkit.modules.walletconnect

import android.net.Uri
import io.horizontalsystems.walletkit.IPinComponent
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.modules.walletconnect.handler.IWCHandler
import io.horizontalsystems.walletkit.modules.walletconnect.handler.MethodData
import io.horizontalsystems.walletkit.modules.walletconnect.request.AbstractWCAction
import io.horizontalsystems.walletkit.modules.walletconnect.session.ValidationError
import io.horizontalsystems.dapp.core.DAppManager
import io.horizontalsystems.dapp.core.HSDAppNamespaceSession
import io.horizontalsystems.dapp.core.HSDAppRequest
import timber.log.Timber

class WCManager(
    private val accountManager: IAccountManager,
    private val pinComponent: IPinComponent,
) {
    sealed class SupportState {
        object Supported : SupportState()
        object NotSupportedDueToNoActiveAccount : SupportState()
        class NotSupported(val accountTypeDescription: String) : SupportState()
    }

    private val handlersMap = mutableMapOf<String, IWCHandler>()

    // A `wc:` URI that arrived (deeplink or QR scan) while the app was locked. Pairing is an
    // active network handshake with a dApp, so it must not run behind the lock screen: it would
    // let anyone holding an OS-unlocked device establish a session the owner never saw, and the
    // proposal sheet then pops up on their next unlock looking self-inflicted. The URI is kept
    // here and paired by flushPendingPairing() once unlocked.
    private var pendingPairingUri: String? = null

    /**
     * Pairs with [uri], or holds it until the app is unlocked. Callbacks apply to the immediate
     * path only — a deferred pairing reports through the usual WC event stream instead.
     */
    fun pairOrDefer(
        uri: String,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ) {
        val trimmed = uri.trim()
        if (pinComponent.isLocked) {
            pendingPairingUri = trimmed
            return
        }
        DAppManager.pair(trimmed, onSuccess, onError)
    }

    /** Pairs with the URI held by [pairOrDefer] while locked, if any. Call after unlocking. */
    fun flushPendingPairing() {
        val uri = pendingPairingUri ?: return
        pendingPairingUri = null
        if (!DAppManager.isAvailable) return

        // Pairing reports asynchronously through the WC delegate (which surfaces errors as an
        // HSDAppEvent.Error hud), but an unusable SDK throws straight away. This runs from a
        // LaunchedEffect on the main dispatcher, where an escaping exception would take the
        // process down and skip the pending-event replay that follows it — so it stops here.
        try {
            DAppManager.pair(
                uri = uri,
                onError = { Timber.e(it, "Deferred WalletConnect pairing failed") }
            )
        } catch (e: Exception) {
            Timber.e(e, "Deferred WalletConnect pairing failed")
        }
    }

    fun addWcHandler(wcHandler: IWCHandler) {
        handlersMap[wcHandler.chainNamespace] = wcHandler
    }

    fun getMethodData(request: HSDAppRequest): MethodData? {
        val chainId = request.chainId ?: return null
        val chainParts = chainId.split(":")

        val chainNamespace = chainParts.getOrNull(0)
        val chainInternalId = chainParts.getOrNull(1)

        val handler = handlersMap[chainNamespace] ?: return null

        return handler.getMethodData(request.method, chainInternalId)
    }

    fun getActionForRequest(request: HSDAppRequest?): AbstractWCAction? {
        if (request == null) return null
        val chainId = request.chainId ?: return null
        val chainParts = chainId.split(":")

        val chainNamespace = chainParts.getOrNull(0)
        val chainInternalId = chainParts.getOrNull(1)

        val handler = handlersMap[chainNamespace] ?: return null

        return handler.getAction(request, chainInternalId)
    }

    // WalletConnect can arrive either as a raw `wc:` URI (QR scan / clipboard) or wrapped in the
    // app's own scheme by the dApp / Web3Modal, e.g. `unstoppable.money://wc?uri=<URL-encoded wc:>`.
    // Returns the embedded `wc:` pairing URI, or null if the deeplink isn't a WalletConnect one.
    fun getWalletConnectUri(deepLink: Uri): String? {
        val deeplinkString = deepLink.toString()
        return when {
            deeplinkString.startsWith("wc:") -> deeplinkString
            deepLink.host == "wc" -> {
                // getQueryParameter isolates and percent-decodes just the `uri` value, so any
                // additional params (e.g. ?uri=<...>&foo=bar) don't leak into the pairing URI.
                deepLink.getQueryParameter("uri")
                    ?.trim()
                    ?.removePrefix("@")
            }

            else -> null
        }
    }

    fun getWalletConnectSupportState(): SupportState {
        val tmpAccount = accountManager.activeAccount
        return when {
            tmpAccount == null -> SupportState.NotSupportedDueToNoActiveAccount
            tmpAccount.type.supportsWalletConnect -> SupportState.Supported
            else -> SupportState.NotSupported(tmpAccount.type.description)
        }
    }

    fun validate(requiredNamespaces: Map<String, io.horizontalsystems.dapp.core.HSDAppNamespaceProposal>) {
        requiredNamespaces.forEach { (chainNamespace, proposal) ->
            val handler = handlersMap[chainNamespace]
                ?: throw ValidationError.UnsupportedChainNamespace(chainNamespace)

            proposal.chains?.let { requiredChains ->
                val unsupportedChains = requiredChains - handler.supportedChains.toSet()
                if (unsupportedChains.isNotEmpty()) {
                    throw ValidationError.UnsupportedChains(unsupportedChains)
                }
            }

            val unsupportedMethods = proposal.methods - handler.supportedMethods.toSet()
            if (unsupportedMethods.isNotEmpty()) {
                throw ValidationError.UnsupportedMethods(unsupportedMethods)
            }

            val unsupportedEvents = proposal.events - handler.supportedEvents.toSet()
            if (unsupportedEvents.isNotEmpty()) {
                throw ValidationError.UnsupportedEvents(unsupportedEvents)
            }
        }
    }

    fun getSupportedNamespaces(account: Account): Map<String, HSDAppNamespaceSession> =
        handlersMap.map { (chainNamespace, handler) ->
            chainNamespace to HSDAppNamespaceSession(
                chains = handler.supportedChains,
                methods = handler.supportedMethods,
                events = handler.supportedEvents,
                accounts = handler.getAccountAddresses(account)
            )
        }.toMap()

    fun getChainNames(namespaces: Map<String, HSDAppNamespaceSession>): List<String> {
        val res = mutableListOf<String>()

        for ((chainNamespace, session) in namespaces) {
            val handler = handlersMap[chainNamespace] ?: continue

            for (accountId in session.accounts) {
                val accountIdParts = accountId.split(":")
                val chainInternalId = accountIdParts.getOrNull(1) ?: continue

                handler.getChainName(chainInternalId)?.let {
                    res.add(it)
                }
            }
        }

        return res
    }
}
