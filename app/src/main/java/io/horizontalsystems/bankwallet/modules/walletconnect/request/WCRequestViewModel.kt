package io.horizontalsystems.bankwallet.modules.walletconnect.request

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.JsonParser
import com.unstoppabledomains.resolution.artifacts.Numeric
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCUtils
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val PERSONAL_SIGN_METHOD = "personal_sign"
private const val TYPED_DATA_METHOD = "eth_signTypedData"
private const val MESSAGE_METHOD = "eth_sign"
private const val SEND_TRANSACTION_METHOD = "eth_sendTransaction"
private const val SIGN_TRANSACTION_METHOD = "eth_signTransaction"

class WCNewRequestViewModel(
    private val accountManager: IAccountManager,
    private val evmBlockchainManager: EvmBlockchainManager,
) : ViewModel() {

    val blockchain: Blockchain? by lazy {
        val sessionChainId = WCDelegate.sessionRequestEvent?.chainId ?: return@lazy null
        val chainId = getChainData(sessionChainId)?.chain?.id ?: return@lazy null
        evmBlockchainManager.getBlockchain(chainId)
    }

    val evmKitWrapper: EvmKitWrapper? = getEthereumKitWrapper()
    var sessionRequest: SessionRequestUI = generateSessionRequestUI()

    private fun clearSessionRequest() {
        sessionRequest = SessionRequestUI.Initial
    }

    private fun generateSessionRequestUI(): SessionRequestUI {
        return WCDelegate.sessionRequestEvent?.let { sessionRequest ->
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
                requestId = sessionRequest.request.id,
                param = getParam(sessionRequest),
                chainData = getChainData(sessionRequest.chainId),
                method = sessionRequest.request.method,
            )
        } ?: SessionRequestUI.Initial
    }

    private fun getParam(sessionRequest: Wallet.Model.SessionRequest) =
        when (sessionRequest.request.method) {
            PERSONAL_SIGN_METHOD -> {
                extractMessageParamFromPersonalSign(sessionRequest.request.params)
            }

            TYPED_DATA_METHOD, SEND_TRANSACTION_METHOD, SIGN_TRANSACTION_METHOD -> {
                val params = JsonParser.parseString(sessionRequest.request.params).asJsonArray
                params.firstOrNull { it.isJsonObject }?.asJsonObject?.toString()
                    ?: throw Exception("Invalid Data")
            }

            else -> {
                sessionRequest.request.params
            }
        }

    private fun getChainData(chainId: String?): WCChainData? {
        return WCUtils.getChainData(chainId ?: return null)
    }

    private fun extractMessageParamFromPersonalSign(input: String): String {
        val jsonArray = JSONArray(input)
        return if (jsonArray.length() > 0) {
            String(Numeric.hexStringToByteArray(jsonArray.getString(0)))
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun getEthereumKitWrapper(): EvmKitWrapper? {
        val blockchain = blockchain ?: return null
        val sessionChainId = WCDelegate.sessionRequestEvent?.chainId ?: return null
        val chainId = getChainData(sessionChainId)?.chain?.id ?: return null

        val account = accountManager.activeAccount ?: return null
        val evmKitManager = evmBlockchainManager.getEvmKitManager(blockchain.type)
        val evmKitWrapper = evmKitManager.getEvmKitWrapper(account, blockchain.type)

        return if (evmKitWrapper.evmKit.chain.id == chainId) {
            evmKitWrapper
        } else {
            evmKitManager.unlink(account)
            null
        }
    }

    suspend fun allow() {
        val evmKit = evmKitWrapper ?: throw WCSessionManager.RequestDataError.NoSuitableEvmKit
        val signer = evmKit.signer ?: throw WCSessionManager.RequestDataError.NoSigner
        return suspendCoroutine { continuation ->
            val sessionRequest = sessionRequest as? SessionRequestUI.Content
            if (sessionRequest != null) {
                Log.e("TAG", "sessionRequest.method: ${sessionRequest.method}")
                val result: String = when {
                    sessionRequest.method == PERSONAL_SIGN_METHOD ||
                            sessionRequest.method == MESSAGE_METHOD -> {
                        signer.signByteArray(message = sessionRequest.param.toByteArray())
                            .toHexString()
                    }

                    sessionRequest.method == TYPED_DATA_METHOD -> {
                        signer.signTypedData(rawJsonMessage = sessionRequest.param).toHexString()
                    }

                    else -> throw Exception("Unsupported Chain")
                }

                WCDelegate.respondPendingRequest(
                    sessionRequest.requestId,
                    sessionRequest.topic,
                    result,
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

    suspend fun reject() {
        return suspendCoroutine { continuation ->
            val sessionRequest = sessionRequest as? SessionRequestUI.Content
            if (sessionRequest != null) {
                WCDelegate.rejectRequest(
                    sessionRequest.topic,
                    sessionRequest.requestId,
                    onSuccessResult = {
                        clearSessionRequest()
                        continuation.resume(Unit)
                    },
                    onErrorResult = {
                        clearSessionRequest()
                        continuation.resumeWithException(it)
                    }
                )
            }
        }
    }

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCNewRequestViewModel(App.accountManager, App.evmBlockchainManager) as T
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
        val chainData: WCChainData?,
        val method: String,
    ) : SessionRequestUI()
}

@Parcelize
data class WCChainData(
    val chain: Chain,
    val address: String?
) : Parcelable

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