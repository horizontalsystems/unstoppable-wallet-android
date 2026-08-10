package io.horizontalsystems.walletkit.modules.walletconnect

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.dapp.core.DAppManager
import io.horizontalsystems.dapp.core.DAppServiceCallback
import io.horizontalsystems.dapp.core.HSDAppEvent
import io.horizontalsystems.dapp.core.HSDAppProposal
import io.horizontalsystems.dapp.core.HSDAppRequest
import io.horizontalsystems.dapp.core.HSDAppSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object WCDelegate : DAppServiceCallback {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _walletEvents = MutableSharedFlow<HSDAppEvent>()
    val walletEvents: SharedFlow<HSDAppEvent> = _walletEvents.asSharedFlow()

    private val _pairingEvents = MutableSharedFlow<Unit>()
    val pairingEvents: SharedFlow<Unit> = _pairingEvents.asSharedFlow()

    private val _pendingRequestEvents = MutableSharedFlow<Unit>()
    val pendingRequestEvents: SharedFlow<Unit> = _pendingRequestEvents.asSharedFlow()

    private val _connectionAvailableEvent = MutableStateFlow<Boolean?>(null)
    val connectionAvailableEvent: StateFlow<Boolean?> = _connectionAvailableEvent.asStateFlow()

    var sessionProposalEvent: HSDAppProposal? = null

    // sessionRequestEvent is touched from WC SDK callback threads, IO coroutines and the main
    // thread (UI reject/discard, lifecycle re-emission). Guard every read/replace/clear behind a
    // monitor lock so observers never see a torn/stale transition. A coroutine Mutex is unusable
    // here because most access points are synchronous (non-suspend) property access.
    private val sessionRequestLock = Any()
    private var _sessionRequestEvent: HSDAppRequest? = null
    var sessionRequestEvent: HSDAppRequest?
        get() = synchronized(sessionRequestLock) { _sessionRequestEvent }
        set(value) = synchronized(sessionRequestLock) { _sessionRequestEvent = value }

    // region DAppServiceCallback

    override fun onConnectionStateChange(isAvailable: Boolean) {
        _connectionAvailableEvent.tryEmit(isAvailable)
        scope.launch { _walletEvents.emit(HSDAppEvent.ConnectionState(isAvailable)) }
    }

    override fun onError(throwable: Throwable) {
        scope.launch { _walletEvents.emit(HSDAppEvent.Error(throwable)) }
    }

    override fun onSessionDelete(topic: String) {
        scope.launch { _walletEvents.emit(HSDAppEvent.SessionDelete(topic)) }
    }

    override fun onSessionProposal(proposal: HSDAppProposal) {
        sessionProposalEvent = proposal
        scope.launch { _walletEvents.emit(HSDAppEvent.SessionProposal(proposal)) }
    }

    override fun onSessionRequest(request: HSDAppRequest) {
        // Do NOT clear sessionRequestEvent before we have a replacement: onSessionRequest can be
        // delivered more than once for the same request, and a duplicate/empty callback would
        // otherwise wipe the request currently shown to the user (dApp then hangs pending).
        val newRequest = App.wcSessionManager.getNewSessionRequest() ?: return
        if (App.wcWalletRequestHandler?.handle(newRequest) == true) return

        sessionRequestEvent = newRequest
        scope.launch {
            _walletEvents.emit(HSDAppEvent.SessionRequest(newRequest))
            _pendingRequestEvents.emit(Unit)
        }
    }

    override fun onSessionSettled(session: HSDAppSession) {
        scope.launch { _walletEvents.emit(HSDAppEvent.SessionSettled(session)) }
    }

    override fun onSessionSettleError(errorMessage: String) {
        scope.launch { _walletEvents.emit(HSDAppEvent.SessionSettleError(errorMessage)) }
    }

    override fun onSessionUpdate() {
        scope.launch { _walletEvents.emit(HSDAppEvent.SessionUpdate) }
    }

    override fun onPairingDelete(topic: String) {
        scope.launch {
            _walletEvents.emit(HSDAppEvent.PairingDelete(topic))
            _pairingEvents.emit(Unit)
        }
    }

    // endregion

    // region Actions (delegated to DAppManager)

    fun getPairings() = DAppManager.getPairings()

    fun getActiveSessions() = DAppManager.getActiveSessions()

    fun deletePairing(topic: String, onError: (Throwable) -> Unit = {}) {
        DAppManager.disconnectPairing(topic, onError)
        scope.launch { _pairingEvents.emit(Unit) }
    }

    fun deleteAllPairings(onError: (Throwable) -> Unit = {}) {
        DAppManager.disconnectAllPairings(onError)
        scope.launch { _pairingEvents.emit(Unit) }
    }

    fun deleteSession(topic: String, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        DAppManager.disconnectSession(
            topic = topic,
            onSuccess = {
                scope.launch { _walletEvents.emit(HSDAppEvent.SessionDelete(topic)) }
                onSuccess()
            },
            onError = onError
        )
    }

    fun respondPendingRequest(
        requestId: Long,
        topic: String,
        data: String,
        onSuccessResult: () -> Unit = {},
        onErrorResult: (Throwable) -> Unit = {},
    ) {
        DAppManager.respondRequest(
            topic = topic,
            requestId = requestId,
            result = data,
            onSuccess = {
                onSuccessResult()
                scope.launch {
                    clearSessionRequestEventIfMatches(requestId)
                    _pendingRequestEvents.emit(Unit)
                }
            },
            onError = { error ->
                clearSessionRequestEventIfMatches(requestId)
                onErrorResult(error)
                scope.launch { _walletEvents.emit(HSDAppEvent.Error(error)) }
            }
        )
    }

    // The user dismissed/rejected the request UI without responding. The request stays in
    // DAppManager's pending list (still reachable from the WC list), but clear the "active request"
    // pointer so reEmitPendingWcEventIfNeeded() doesn't auto-reopen it on the next resume (removing
    // the bottom sheet overlay re-resumes the screen underneath). Pass the displayed request's id so
    // a newer request that became active while the sheet was shown is not discarded by mistake.
    fun discardActiveSessionRequest(requestId: Long) {
        clearSessionRequestEventIfMatches(requestId)
    }

    // No-id variant for the error screen, which is shown when there is no resolvable request to
    // identify (unsupported chain / missing event); clears whatever stale pointer remains.
    fun discardActiveSessionRequest() {
        sessionRequestEvent = null
    }

    // Clear the active request only if it is still the one being responded to/rejected. respond and
    // reject callbacks arrive asynchronously, so by the time one lands a newer request may already
    // be active; an unconditional clear would wipe it. Compound check-and-clear under the lock.
    private fun clearSessionRequestEventIfMatches(requestId: Long) {
        synchronized(sessionRequestLock) {
            if (_sessionRequestEvent?.requestId == requestId) {
                _sessionRequestEvent = null
            }
        }
    }

    fun rejectRequest(
        topic: String,
        requestId: Long,
        onSuccessResult: () -> Unit = {},
        onErrorResult: (Throwable) -> Unit = {},
    ) {
        DAppManager.rejectRequest(
            topic = topic,
            requestId = requestId,
            onSuccess = {
                onSuccessResult()
                scope.launch {
                    clearSessionRequestEventIfMatches(requestId)
                    _pendingRequestEvents.emit(Unit)
                }
            },
            onError = { error ->
                clearSessionRequestEventIfMatches(requestId)
                onErrorResult(error)
                scope.launch { _walletEvents.emit(HSDAppEvent.Error(error)) }
            }
        )
    }

    // endregion
}
