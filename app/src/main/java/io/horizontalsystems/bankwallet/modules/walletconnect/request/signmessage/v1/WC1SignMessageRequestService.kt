package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v1

import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage.WCSignType
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestModule.SignMessage
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SignMessageRequest
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.signer.Signer

class WC1SignMessageRequestService(
    private val request: WC1SignMessageRequest,
    private val baseService: WC1Service,
    private val signer: Signer
) : WCSignMessageRequestModule.RequestAction {

    private val evmKitWrapper: EvmKitWrapper?
        get() = baseService.evmKitWrapper

    override val message: SignMessage by lazy {
        val messageData = request.message.data
        when (request.message.type) {
            WCSignType.MESSAGE -> SignMessage.Message(hexStringToUtf8String(messageData))
            WCSignType.PERSONAL_MESSAGE -> SignMessage.PersonalMessage(
                hexStringToUtf8String(messageData)
            )
            WCSignType.TYPED_MESSAGE -> {
                val typeData = signer.parseTypedData(messageData)
                val domain = typeData?.domain?.get("name")?.toString()
                SignMessage.TypedMessage(messageData, domain ?: "", null)
            }
        }
    }

    private fun hexStringToUtf8String(hexString: String) = try {
        String(hexString.hexStringToByteArray())
    } catch (_: Throwable) {
        hexString
    }

    private fun signMessage(message: WCEthereumSignMessage): ByteArray? {
        return evmKitWrapper?.evmKit?.let { evmKit ->
            when (message.type) {
                WCSignType.MESSAGE,
                WCSignType.PERSONAL_MESSAGE -> {
                    signer.signByteArray(message = message.data.hexStringToByteArray())
                }
                WCSignType.TYPED_MESSAGE -> {
                    signer.signTypedData(rawJsonMessage = message.data)
                }
            }
        }
    }

    override fun sign() {
        val signedMessage = signMessage(request.message)
        baseService.approveRequest(request.id, signedMessage ?: byteArrayOf())
    }

    override fun reject() {
        baseService.rejectRequest(request.id)
    }

}
