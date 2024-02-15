package io.horizontalsystems.bankwallet.modules.walletconnect

import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
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

object WCDelegate : Web3Wallet.WalletDelegate, CoreClient.CoreDelegate {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _coreEvents: MutableSharedFlow<Core.Model> = MutableSharedFlow()
    val coreEvents: SharedFlow<Core.Model> = _coreEvents.asSharedFlow()

    private val _pairingEvents: MutableSharedFlow<Unit> = MutableSharedFlow()
    val pairingEvents: SharedFlow<Unit> = _pairingEvents.asSharedFlow()

    private val _walletEvents: MutableSharedFlow<Wallet.Model> = MutableSharedFlow()
    val walletEvents: SharedFlow<Wallet.Model> = _walletEvents.asSharedFlow()

    private val _pendingRequestEvents: MutableSharedFlow<Unit> = MutableSharedFlow()
    val pendingRequestEvents: SharedFlow<Unit> = _pendingRequestEvents.asSharedFlow()

    private val _connectionAvailableEvent: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val connectionAvailableEvent: StateFlow<Boolean?> = _connectionAvailableEvent.asStateFlow()

    var authRequestEvent: Pair<Wallet.Model.AuthRequest, Wallet.Model.VerifyContext>? = null
    var sessionProposalEvent: Pair<Wallet.Model.SessionProposal, Wallet.Model.VerifyContext>? = null
    var sessionRequestEvent: Wallet.Model.SessionRequest? = null

    init {
        CoreClient.setDelegate(this)
        Web3Wallet.setWalletDelegate(this)
    }

    override fun onAuthRequest(
        authRequest: Wallet.Model.AuthRequest,
        verifyContext: Wallet.Model.VerifyContext
    ) {
        authRequestEvent = Pair(authRequest, verifyContext)

        scope.launch {
            _walletEvents.emit(authRequest)
        }
    }

    override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
        scope.launch {
            _connectionAvailableEvent.emit(state.isAvailable)
        }
        scope.launch {
            _walletEvents.emit(state)
        }
    }


    override fun onError(error: Wallet.Model.Error) {
        scope.launch {
            _walletEvents.emit(error)
        }
    }

    override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
        scope.launch {
            _walletEvents.emit(sessionDelete)
        }
    }

//    override fun onSessionExtend(session: Wallet.Model.Session) {
//        Log.d("Session Extend", "${session.expiry}")
//    }

    override fun onSessionProposal(
        sessionProposal: Wallet.Model.SessionProposal,
        verifyContext: Wallet.Model.VerifyContext
    ) {
        sessionProposalEvent = Pair(sessionProposal, verifyContext)

        scope.launch {
            _walletEvents.emit(sessionProposal)
        }
    }

    override fun onSessionRequest(
        sessionRequest: Wallet.Model.SessionRequest,
        verifyContext: Wallet.Model.VerifyContext
    ) {
        sessionRequestEvent = sessionRequest

        scope.launch {
            _walletEvents.emit(sessionRequest)
        }
    }

    override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
        scope.launch {
            _walletEvents.emit(settleSessionResponse)
        }
    }

    override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
        scope.launch {
            _walletEvents.emit(sessionUpdateResponse)
        }
    }

    override fun onPairingDelete(deletedPairing: Core.Model.DeletedPairing) {
        // not working during pairing delete
        scope.launch {
            _coreEvents.emit(deletedPairing)
        }
    }

//    override fun onProposalExpired(proposal: Wallet.Model.ExpiredProposal) {
//        Log.e("TAG", "onProposalExpired: ", )
//    }
//
//    override fun onRequestExpired(request: Wallet.Model.ExpiredRequest) {
//        Log.e("TAG", "onRequestExpired: ", )
//    }
//
//    override fun onSessionExtend(session: Wallet.Model.Session) {
//        Log.e("TAG", "onSessionExtend: ", )
//    }
//
//    override fun onPairingExpired(expiredPairing: Core.Model.ExpiredPairing) {
//        Log.e("TAG", "onPairingExpired: ", )
//    }
//
//    override fun onPairingState(pairingState: Core.Model.PairingState) {
//        Log.e("TAG", "onPairingState: $pairingState", )
//    }

//    fun deleteAccountAllPairings(currentAccountTopics: List<String>) {
//        Web3Wallet.getListOfActiveSessions()
//            .filter { currentAccountTopics.contains(it.topic) }
//            .forEach {
//                deletePairing(it.topic)
//            }
//    }

    fun getPairings(): List<Core.Model.Pairing> {
        return CoreClient.Pairing.getPairings()
    }

    fun getActiveSessions(): List<Wallet.Model.Session> {
        return Web3Wallet.getListOfActiveSessions()
    }

    fun deletePairing(topic: String, onError: (Throwable) -> Unit = {}) {
        val params = Core.Params.Disconnect(topic)
        CoreClient.Pairing.disconnect(params, onError = {
            onError.invoke(it.throwable)
        })
        scope.launch {
            _pairingEvents.emit(Unit)
        }
    }

    fun deleteAllPairings(onError: (Throwable) -> Unit = {}) {
        try {
            CoreClient.Pairing.getPairings().forEach {
                deletePairing(it.topic)
            }
        } catch (e: Exception) {
            onError.invoke(e)
        }
        scope.launch {
            _pairingEvents.emit(Unit)
        }
    }

    fun deleteSession(
        topic: String,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        Web3Wallet.disconnectSession(Wallet.Params.SessionDisconnect(topic),
            onSuccess = {
                scope.launch {
                    _walletEvents.emit(Wallet.Model.SessionDelete.Success(it.sessionTopic, ""))
                }
                onSuccess.invoke()
            },
            onError = {
                onError.invoke(it.throwable)
            })
    }

    fun respondPendingRequest(
        requestId: Long,
        topic: String,
        data: String,
        onSuccessResult: () -> Unit = {},
        onErrorResult: (Throwable) -> Unit = {},
        ) {
        val response = Wallet.Params.SessionRequestResponse(
            sessionTopic = topic,
            jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(requestId, data)
        )

        Web3Wallet.respondSessionRequest(response,
            onSuccess = {
                onSuccessResult.invoke()
                scope.launch {
                    sessionRequestEvent = null
                    _pendingRequestEvents.emit(Unit)
                }
            },
            onError = { error ->
                sessionRequestEvent = null
                onErrorResult.invoke(error.throwable)
                onError(error)
            })
    }

    fun rejectRequest(
        topic: String,
        requestId: Long,
        onSuccessResult: () -> Unit = {},
        onErrorResult: (Throwable) -> Unit = {},
        ) {
        val result = Wallet.Params.SessionRequestResponse(
            sessionTopic = topic,
            jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                id = requestId,
                code = 500,
                message = "Unstoppable Wallet Error"
            )
        )

        Web3Wallet.respondSessionRequest(result,
            onSuccess = {
                onSuccessResult.invoke()
                scope.launch {
                    sessionRequestEvent = null
                    _pendingRequestEvents.emit(Unit)
                }
            },
            onError = { error ->
                sessionRequestEvent = null
                onErrorResult.invoke(error.throwable)
                onError(error)
            })
    }

}