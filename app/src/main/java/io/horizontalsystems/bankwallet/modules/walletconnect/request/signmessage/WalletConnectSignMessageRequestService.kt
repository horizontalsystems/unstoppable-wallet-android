package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage

import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage.WCSignType
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSignMessageRequest
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray

class WalletConnectSignMessageRequestService(
        private val request: WalletConnectSignMessageRequest,
        private val baseService: WalletConnectService
) {

    private val evmKit: EthereumKit?
        get() = baseService.evmKit

    val message: SignMessage by lazy {
        val messageData = request.message.data
        when (request.message.type) {
            WCSignType.MESSAGE -> SignMessage.Message(hexStringToUtf8String(messageData))
            WCSignType.PERSONAL_MESSAGE -> SignMessage.PersonalMessage(hexStringToUtf8String(messageData))
            WCSignType.TYPED_MESSAGE -> {
                val typeData = evmKit?.parseTypedData(messageData)
                val domain = typeData?.domain?.get("name")?.toString()
                SignMessage.TypedMessage(messageData, domain ?: "")
            }
        }
    }

    private fun hexStringToUtf8String(hexString: String) = try {
        String(hexString.hexStringToByteArray())
    } catch (_: Throwable) {
        hexString
    }

    private fun signMessage(message: WCEthereumSignMessage): ByteArray? {
        return evmKit?.let { evmKit ->
            when (message.type) {
                WCSignType.MESSAGE,
                WCSignType.PERSONAL_MESSAGE -> {
                    evmKit.signByteArray(message = message.data.hexStringToByteArray())
                }
                WCSignType.TYPED_MESSAGE -> {
                    evmKit.signTypedData(rawJsonMessage = message.data)
                }
            }
        }
    }

    fun sign() {
        val signedMessage = signMessage(request.message)
        baseService.approveRequest(request.id, signedMessage ?: byteArrayOf())
    }

    fun reject() {
        baseService.rejectRequest(request.id)
    }

    sealed class SignMessage(val data: String) {
        class Message(data: String) : SignMessage(data)
        class PersonalMessage(data: String) : SignMessage(data)
        class TypedMessage(data: String, val domain: String) : SignMessage(data)
    }

}
