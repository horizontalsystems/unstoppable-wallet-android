package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v1

import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage.WCSignType
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCRequestChain
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.SignMessage
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SignMessageRequest
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.signer.Signer

class WC1SignMessageRequestService(
    private val request: WC1SignMessageRequest,
    override val dAppName: String?,
    private val baseService: WC1Service,
    private val signer: Signer
) : WCSignMessageRequestModule.RequestAction {

    override val chain: WCRequestChain by lazy {
        val blockchainName = baseService.selectedBlockchain.name
        val address = baseService.evmKitWrapper?.evmKit?.receiveAddress?.eip55?.shorten() ?: ""
        WCRequestChain(blockchainName, address)
    }

    override val isLegacySignRequest =
        request.message.type == WCSignType.MESSAGE && request.message.data.hexStringToByteArray().size == 32

    override val message: SignMessage by lazy {
        val messageData = request.message.data
        when (request.message.type) {
            WCSignType.MESSAGE -> {
                if (isLegacySignRequest) {
                    SignMessage.Message(messageData, true)
                } else {
                    SignMessage.Message(hexStringToUtf8String(messageData))
                }
            }
            WCSignType.PERSONAL_MESSAGE -> SignMessage.PersonalMessage(
                hexStringToUtf8String(messageData)
            )
            WCSignType.TYPED_MESSAGE -> {
                val typeData = signer.parseTypedData(messageData)
                val domain = typeData?.domain?.get("name")?.toString()
                val sanitizedMessage = try {
                    typeData?.sanitizedMessage ?: messageData
                } catch (error: Throwable) {
                    messageData
                }
                SignMessage.TypedMessage(sanitizedMessage, domain)
            }
        }
    }

    override fun sign() {
        val signedMessage = signMessage(request.message)
        baseService.approveRequest(request.id, signedMessage)
    }

    override fun reject() {
        baseService.rejectRequest(request.id)
    }

    private fun hexStringToUtf8String(hexString: String) = try {
        String(hexString.hexStringToByteArray())
    } catch (_: Throwable) {
        hexString
    }

    private fun signMessage(message: WCEthereumSignMessage): ByteArray {
        return when (message.type) {
            WCSignType.MESSAGE -> {
                if (isLegacySignRequest) {
                    signer.signByteArrayLegacy(message = message.data.hexStringToByteArray())
                } else {
                    signer.signByteArray(message = message.data.hexStringToByteArray())
                }
            }
            WCSignType.PERSONAL_MESSAGE -> {
                signer.signByteArray(message = message.data.hexStringToByteArray())
            }
            WCSignType.TYPED_MESSAGE -> {
                signer.signTypedData(rawJsonMessage = message.data)
            }
        }
    }

}
