package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2

import com.walletconnect.walletconnectv2.client.WalletConnect
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestModule.SignMessage
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Manager
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Parser
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray

class WC2SignMessageRequestService(
    private val requestId: Long,
    private val wc2Manager: WC2Manager,
    private val accountManager: IAccountManager,
    private val sessionManager: WC2SessionManager,
) : WCSignMessageRequestModule.RequestAction {

    private val pendingRequest by lazy {
        sessionManager.pendingRequest(requestId)
    }

    private fun evmKitWrapper(chainId: Int): EvmKitWrapper? {
        val account = accountManager.activeAccount ?: return null
        return wc2Manager.evmKitWrapper(chainId, account)
    }

    override val message: SignMessage? by lazy {
        getSignMessage(pendingRequest)
    }

    private fun getSignMessage(pendingRequest: WalletConnect.Model.JsonRpcHistory.HistoryEntry?): SignMessage? {
        val request = pendingRequest ?: return null
        val dAppName = sessionManager.sessionByTopic(request.topic)?.peerAppMetaData?.name
        val chainId = WC2Parser.getChainIdFromBody(request.body) ?: return null
        val evmKitWrapper = evmKitWrapper(chainId) ?: return null
        val requestMethod = WC2Parser.getSessionRequestMethod(request.body)
        val data =
            WC2Parser.getMessageData(request.body, evmKitWrapper.evmKit.receiveAddress.eip55)

        return when (requestMethod) {
            "personal_sign" -> SignMessage.PersonalMessage(
                hexStringToUtf8String(data ?: "")
            )
            "eth_signTypedData" -> {
                val domain = WC2Parser.getSessionRequestDomainName(data) ?: ""
                SignMessage.TypedMessage(data ?: "", domain, dAppName)
            }
            else -> null
        }
    }

    private fun hexStringToUtf8String(hexString: String) = try {
        String(hexString.hexStringToByteArray())
    } catch (_: Throwable) {
        hexString
    }

    override fun sign() {
        pendingRequest?.let { request ->
            val data = request.body ?: return
            sessionManager.service.respondPendingRequest(
                request.requestId,
                request.topic,
                data
            )
        }
    }

    override fun reject() {
        pendingRequest?.let {
            sessionManager.service.rejectRequest(it.topic, it.requestId)
        }
    }
}
