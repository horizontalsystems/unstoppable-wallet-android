package io.horizontalsystems.dapp.walletconnect

import android.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import io.horizontalsystems.dapp.core.DAppInitParams
import io.horizontalsystems.dapp.core.DAppService
import io.horizontalsystems.dapp.core.DAppServiceCallback
import io.horizontalsystems.dapp.core.HSDAppAppMetaData
import io.horizontalsystems.dapp.core.HSDAppNamespaceProposal
import io.horizontalsystems.dapp.core.HSDAppNamespaceSession
import io.horizontalsystems.dapp.core.HSDAppPairing
import io.horizontalsystems.dapp.core.HSDAppProposal
import io.horizontalsystems.dapp.core.HSDAppRequest
import io.horizontalsystems.dapp.core.HSDAppSession

class DAppServiceWalletConnect : DAppService, WalletKit.WalletDelegate, CoreClient.CoreDelegate {

    private var callback: DAppServiceCallback? = null

    override fun initialize(params: DAppInitParams, callback: DAppServiceCallback) {
        this.callback = callback

        val appMetaData = Core.Model.AppMetaData(
            name = params.appName,
            description = "",
            url = params.appUrl,
            icons = listOf(params.appIcon),
            redirect = null,
        )

        CoreClient.initialize(
            metaData = appMetaData,
            relayServerUrl = params.relayServerUrl,
            connectionType = ConnectionType.AUTOMATIC,
            application = params.application,
            onError = { error -> Log.w("DAppServiceWC", "CoreClient init error: ${error.throwable}") },
        )

        WalletKit.initialize(Wallet.Params.Init(core = CoreClient)) { error ->
            Log.e("DAppServiceWC", "WalletKit init error: ${error.throwable}")
        }

        CoreClient.setDelegate(this)
        WalletKit.setWalletDelegate(this)
    }

    // region DAppService — Pairings

    override fun getPairings(): List<HSDAppPairing> =
        CoreClient.Pairing.getPairings().map { it.toHS() }

    override fun disconnectPairing(topic: String, onError: (Throwable) -> Unit) {
        CoreClient.Pairing.disconnect(
            Core.Params.Disconnect(topic),
            onError = { onError(it.throwable) }
        )
    }

    override fun disconnectAllPairings(onError: (Throwable) -> Unit) {
        try {
            CoreClient.Pairing.getPairings().forEach { pairing ->
                disconnectPairing(pairing.topic)
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    // endregion

    // region DAppService — Sessions

    override fun getActiveSessions(): List<HSDAppSession> =
        WalletKit.getListOfActiveSessions().map { it.toHS() }

    override fun disconnectSession(topic: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        WalletKit.disconnectSession(
            params = Wallet.Params.SessionDisconnect(topic),
            onSuccess = { onSuccess() },
            onError = { onError(it.throwable) }
        )
    }

    // endregion

    // region DAppService — Requests

    override fun getPendingRequests(topic: String): List<HSDAppRequest> =
        WalletKit.getPendingListOfSessionRequests(topic).map { it.toHS() }

    override fun respondRequest(topic: String, requestId: Long, result: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val response = Wallet.Params.SessionRequestResponse(
            sessionTopic = topic,
            jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(requestId, result)
        )
        WalletKit.respondSessionRequest(
            params = response,
            onSuccess = { onSuccess() },
            onError = { onError(it.throwable) }
        )
    }

    override fun rejectRequest(topic: String, requestId: Long, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val response = Wallet.Params.SessionRequestResponse(
            sessionTopic = topic,
            jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                id = requestId,
                code = 500,
                message = "Rejected by user"
            )
        )
        WalletKit.respondSessionRequest(
            params = response,
            onSuccess = { onSuccess() },
            onError = { onError(it.throwable) }
        )
    }

    // endregion

    // region DAppService — Session proposals

    override fun getSessionProposals(): List<HSDAppProposal> =
        WalletKit.getSessionProposals().map { it.toHS() }

    override fun generateApprovedNamespaces(
        proposerPublicKey: String,
        supportedNamespaces: Map<String, HSDAppNamespaceSession>
    ): Map<String, HSDAppNamespaceSession> {
        val proposal = WalletKit.getSessionProposals()
            .find { it.proposerPublicKey == proposerPublicKey }
            ?: return emptyMap()

        return WalletKit.generateApprovedNamespaces(
            sessionProposal = proposal,
            supportedNamespaces = supportedNamespaces.toWC()
        ).toHS()
    }

    override fun approveSession(
        proposerPublicKey: String,
        namespaces: Map<String, HSDAppNamespaceSession>,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val proposal = WalletKit.getSessionProposals()
            .find { it.proposerPublicKey == proposerPublicKey }
            ?: run { onError(IllegalStateException("Proposal not found: $proposerPublicKey")); return }

        val approveParams = Wallet.Params.SessionApprove(
            proposerPublicKey = proposal.proposerPublicKey,
            namespaces = namespaces.toWC()
        )
        WalletKit.approveSession(
            params = approveParams,
            onSuccess = { onSuccess() },
            onError = { onError(it.throwable) }
        )
    }

    override fun rejectSession(
        proposerPublicKey: String,
        reason: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val proposal = WalletKit.getSessionProposals()
            .find { it.proposerPublicKey == proposerPublicKey }
            ?: run { onError(IllegalStateException("Proposal not found: $proposerPublicKey")); return }

        val rejectParams = Wallet.Params.SessionReject(
            proposerPublicKey = proposal.proposerPublicKey,
            reason = reason
        )
        WalletKit.rejectSession(
            params = rejectParams,
            onSuccess = { onSuccess() },
            onError = { onError(it.throwable) }
        )
    }

    // endregion

    // region DAppService — Pairing

    override fun pair(uri: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        WalletKit.pair(
            params = Wallet.Params.Pair(uri.trim()),
            onSuccess = { onSuccess() },
            onError = { onError(it.throwable) }
        )
    }

    // endregion

    // region WalletKit.WalletDelegate

    override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
        callback?.onConnectionStateChange(state.isAvailable)
    }

    override fun onError(error: Wallet.Model.Error) {
        callback?.onError(error.throwable)
    }

    override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
        val topic = when (sessionDelete) {
            is Wallet.Model.SessionDelete.Success -> sessionDelete.topic
            is Wallet.Model.SessionDelete.Error -> null
        }
        topic?.let { callback?.onSessionDelete(it) }
    }

    override fun onSessionExtend(session: Wallet.Model.Session) = Unit

    override fun onSessionProposal(
        sessionProposal: Wallet.Model.SessionProposal,
        verifyContext: Wallet.Model.VerifyContext
    ) {
        callback?.onSessionProposal(sessionProposal.toHS())
    }

    override fun onSessionRequest(
        sessionRequest: Wallet.Model.SessionRequest,
        verifyContext: Wallet.Model.VerifyContext
    ) {
        callback?.onSessionRequest(sessionRequest.toHS())
    }

    override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
        when (settleSessionResponse) {
            is Wallet.Model.SettledSessionResponse.Result -> callback?.onSessionSettled(settleSessionResponse.session.toHS())
            is Wallet.Model.SettledSessionResponse.Error -> callback?.onSessionSettleError(settleSessionResponse.errorMessage)
        }
    }

    override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
        callback?.onSessionUpdate()
    }

    // endregion

    // region CoreClient.CoreDelegate

    override fun onPairingDelete(deletedPairing: Core.Model.DeletedPairing) {
        callback?.onPairingDelete(deletedPairing.topic)
    }

    // endregion

    // region Type mappers — WC → HS

    private fun Core.Model.AppMetaData.toHS() = HSDAppAppMetaData(
        name = name,
        description = description,
        url = url,
        icons = icons,
    )

    private fun Core.Model.Pairing.toHS() = HSDAppPairing(
        topic = topic,
        peerAppMetaData = peerAppMetaData?.toHS(),
    )

    private fun Wallet.Model.Session.toHS() = HSDAppSession(
        topic = topic,
        metaData = metaData?.let {
            HSDAppAppMetaData(
                name = it.name,
                description = it.description,
                url = it.url,
                icons = it.icons,
            )
        },
        namespaces = namespaces.mapValues { (_, ns) -> ns.toHS() },
    )

    private fun Wallet.Model.Namespace.Session.toHS() = HSDAppNamespaceSession(
        chains = chains,
        methods = methods,
        events = events,
        accounts = accounts,
    )

    private fun Wallet.Model.Namespace.Proposal.toHS() = HSDAppNamespaceProposal(
        chains = chains,
        methods = methods,
        events = events,
    )

    private fun Wallet.Model.SessionRequest.toHS() = HSDAppRequest(
        topic = topic,
        chainId = chainId,
        method = request.method,
        requestId = request.id,
        params = request.params,
        peerMetaData = peerMetaData?.toHS(),
    )

    private fun Wallet.Model.SessionProposal.toHS() = HSDAppProposal(
        proposerPublicKey = proposerPublicKey,
        name = name,
        url = url,
        description = description,
        icons = icons.map { it.toString() },
        requiredNamespaces = requiredNamespaces.mapValues { (_, ns) -> ns.toHS() },
        optionalNamespaces = optionalNamespaces.mapValues { (_, ns) -> ns.toHS() },
    )

    // endregion

    // region Type mappers — HS → WC

    private fun HSDAppNamespaceSession.toWC() = Wallet.Model.Namespace.Session(
        chains = chains,
        methods = methods,
        events = events,
        accounts = accounts,
    )

    private fun Map<String, HSDAppNamespaceSession>.toWC(): Map<String, Wallet.Model.Namespace.Session> =
        mapValues { (_, ns) -> ns.toWC() }

    private fun Map<String, Wallet.Model.Namespace.Session>.toHS(): Map<String, HSDAppNamespaceSession> =
        mapValues { (_, ns) -> ns.toHS() }

    // endregion
}
