package io.horizontalsystems.walletkit.modules.walletconnect.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.JsonParser
import io.horizontalsystems.walletkit.core.managers.EvmKitManagerRegistry
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.core.managers.EvmKitWrapper
import io.horizontalsystems.walletkit.core.toHexString
import io.horizontalsystems.walletkit.modules.walletconnect.WCDelegate
import io.horizontalsystems.walletkit.modules.walletconnect.WCManager
import io.horizontalsystems.walletkit.modules.walletconnect.WCSessionManager
import io.horizontalsystems.dapp.core.HSDAppRequest
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val PERSONAL_SIGN_METHOD = "personal_sign"
private const val TYPED_DATA_METHOD = "eth_signTypedData"
private const val TYPED_DATA_METHOD_V4 = "eth_signTypedData_v4"
private const val ETH_SIGN_METHOD = "eth_sign"
private const val SEND_TRANSACTION_METHOD = "eth_sendTransaction"
private const val SIGN_TRANSACTION_METHOD = "eth_signTransaction"

class WCRequestEvmViewModel(
    private val accountManager: IAccountManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val wcManager: WCManager,
) : ViewModel() {

    private val sessionRequestEvent = WCDelegate.sessionRequestEvent

    val blockchainType = wcManager.getBlockchainType(sessionRequestEvent?.chainId)
    private val chainData = sessionRequestEvent?.let {
        wcManager.getChainData(it.chainId)
    }
    private val chainName = chainData?.name
    private val chainAddress = chainData?.address

    private val evmKitWrapper: EvmKitWrapper? = getEthereumKitWrapper()
    var sessionRequestUi: SessionRequestUI = generateSessionRequestUI()

    private fun clearSessionRequest() {
        sessionRequestUi = SessionRequestUI.Initial
    }

    private fun generateSessionRequestUI(): SessionRequestUI {
        return sessionRequestEvent?.let { sessionRequest ->
            if (evmKitWrapper == null) {
                clearSessionRequest()
                return@let SessionRequestUI.Initial
            }

            SessionRequestUI.Content(
                peerUI = PeerUI(
                    peerName = sessionRequest.peerMetaData?.name ?: "",
                    peerIcon = sessionRequest.peerMetaData?.icons?.firstOrNull() ?: "",
                    peerUri = sessionRequest.peerMetaData?.url ?: "",
                    peerDescription = sessionRequest.peerMetaData?.description ?: "",
                ),
                topic = sessionRequest.topic,
                requestId = sessionRequest.requestId,
                param = getParam(sessionRequest),
                method = sessionRequest.method,
                chainName = chainName,
                chainAddress = chainAddress,
                typedData = getTypedData(sessionRequest),
                sessionChainId = chainData?.id,
                walletName = accountManager.activeAccount?.name ?: ""
            )
        } ?: SessionRequestUI.Initial
    }

    // Parsed for display only. The signer keeps receiving the untouched request payload, so what
    // is signed never depends on whether this succeeds.
    private fun getTypedData(sessionRequest: HSDAppRequest): Eip712TypedData? =
        when (sessionRequest.method) {
            TYPED_DATA_METHOD, TYPED_DATA_METHOD_V4 -> Eip712Parser.parse(getParam(sessionRequest))
            else -> null
        }

    private fun getParam(sessionRequest: HSDAppRequest) =
        when (sessionRequest.method) {
            PERSONAL_SIGN_METHOD -> {
                extractMessageParamFromPersonalSign(sessionRequest.params)
            }

            ETH_SIGN_METHOD -> {
                val params = JsonParser.parseString(sessionRequest.params).asJsonArray
                if (params.size() >= 2) {
                    params.get(1).asString
                } else {
                    throw Exception("Invalid Data")
                }
            }

            TYPED_DATA_METHOD, TYPED_DATA_METHOD_V4, SEND_TRANSACTION_METHOD, SIGN_TRANSACTION_METHOD -> {
                val params = JsonParser.parseString(sessionRequest.params).asJsonArray
                params.firstOrNull { it.isJsonObject }?.asJsonObject?.toString()
                    ?: throw Exception("Invalid Data")
            }

            else -> {
                sessionRequest.params
            }
        }

    private fun extractMessageParamFromPersonalSign(input: String): String {
        val jsonArray = JSONArray(input)
        return if (jsonArray.length() > 0) {
            val message = jsonArray.getString(0)
            try {
                String(message.hexStringToByteArray())
            } catch (_: Throwable) {
                message
            }
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun getEthereumKitWrapper(): EvmKitWrapper? {
        val blockchainType = blockchainType ?: return null
        val account = accountManager.activeAccount ?: return null
        val evmKitManager = EvmKitManagerRegistry.getEvmKitManager(blockchainType)

        return evmKitManager.getEvmKitWrapper(account, blockchainType)
    }

    suspend fun allow() {
        val evmKit = evmKitWrapper ?: throw WCSessionManager.RequestDataError.NoSuitableEvmKit
        val signer = evmKit.signer ?: throw WCSessionManager.RequestDataError.NoSigner
        return suspendCoroutine { continuation ->
            val sessionRequest = sessionRequestUi as? SessionRequestUI.Content
            if (sessionRequest != null) {
                val result = when (sessionRequest.method) {
                    ETH_SIGN_METHOD -> {
                        val message = sessionRequest.param.hexStringToByteArray()
                        if (message.size == 32) {
                            signer.signByteArrayLegacy(message = message)
                        } else {
                            signer.signByteArray(message = message)
                        }
                    }

                    PERSONAL_SIGN_METHOD -> {
                        signer.signByteArray(message = sessionRequest.param.toByteArray())
                    }

                    TYPED_DATA_METHOD, TYPED_DATA_METHOD_V4 -> {
                        signer.signTypedData(rawJsonMessage = sessionRequest.param)
                    }

                    else -> throw Exception("Unsupported Chain")
                }

                WCDelegate.discardActiveSessionRequest(sessionRequest.requestId)
                WCDelegate.respondPendingRequest(
                    sessionRequest.requestId,
                    sessionRequest.topic,
                    result.toHexString(),
                    onSuccessResult = {
                        continuation.resume(Unit)
                        clearSessionRequest()
                    },
                    onErrorResult = {
                        continuation.resumeWithException(it)
                        clearSessionRequest()
                    }
                )
            }
        }
    }

    fun reject() {
        val sessionRequest = sessionRequestUi as? SessionRequestUI.Content ?: return
        WCDelegate.discardActiveSessionRequest(sessionRequest.requestId)
        WCDelegate.rejectRequest(sessionRequest.topic, sessionRequest.requestId)
        clearSessionRequest()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCRequestEvmViewModel(
                App.accountManager,
                App.evmBlockchainManager,
                App.wcManager
            ) as T
        }
    }
}

sealed class SessionRequestUI {
    object Initial : SessionRequestUI()

    data class Content(
        val peerUI: PeerUI,
        val topic: String,
        val requestId: Long,
        val param: String,
        val method: String,
        val chainName: String?,
        val chainAddress: String?,
        val typedData: Eip712TypedData? = null,
        val sessionChainId: Int? = null,
        val walletName: String,
    ) : SessionRequestUI() {

        /**
         * True when the payload is bound to a different chain than the session it arrived on. The
         * signature would then be valid somewhere the user did not think they were acting.
         */
        val chainIdMismatch: Boolean
            get() {
                val payloadChainId = typedData?.chainId ?: return false
                val sessionId = sessionChainId ?: return false
                return payloadChainId != sessionId.toLong()
            }
    }
}

data class WCChainData(
    val id: Int,
    val name: String,
    val address: String?
)

data class PeerUI(
    val peerIcon: String,
    val peerName: String,
    val peerUri: String,
    val peerDescription: String,
) {
    companion object {
        val Empty = PeerUI("", "", "", "")
    }
}