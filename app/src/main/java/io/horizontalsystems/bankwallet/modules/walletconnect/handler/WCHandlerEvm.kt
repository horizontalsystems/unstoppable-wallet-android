package io.horizontalsystems.bankwallet.modules.walletconnect.handler

import com.walletconnect.android.Core
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain

class WCHandlerEvm(
    private val evmBlockchainManager: EvmBlockchainManager
) : IWCHandler {
    private val supportedEvmChains = EvmBlockchainManager.blockchainTypes.map { evmBlockchainManager.getChain(it) }

    override val chainNamespace = "eip155"

    override val supportedChains = supportedEvmChains.map { "${chainNamespace}:${it.id}" }
    override val supportedMethods = listOf(
        "eth_sendTransaction",
        "personal_sign",
//        "eth_accounts",
//        "eth_requestAccounts",
//        "eth_call",
//        "eth_getBalance",
//        "eth_sendRawTransaction",
        "eth_sign",
        "eth_signTransaction",
        "eth_signTypedData",
        "eth_signTypedData_v4",
        "wallet_addEthereumChain",
        "wallet_switchEthereumChain"
    )

    override val supportedEvents = listOf("chainChanged", "accountsChanged", "connect", "disconnect", "message")

    override fun getAccountAddresses(account: Account): List<String> {
        return supportedEvmChains.map { evmChain ->
            val address = getEvmAddress(account, evmChain)

            "${chainNamespace}:${evmChain.id}:${address.eip55}"
        }
    }

    override fun getMethodData(method: String, chainInternalId: String?): MethodData {
        val evmChain = supportedEvmChains.firstOrNull { it.id == chainInternalId?.toInt() }

        val title = when (method) {
            "personal_sign" -> "Personal Sign Request"
            "eth_sign" -> "Standard Sign Request"
            "eth_signTypedData" -> "Typed Sign Request"
            "eth_sendTransaction" -> "Approve Transaction"
            "eth_signTransaction" -> "Sign Transaction"
            else -> method
        }

        val shortTitle = when (method) {
            "personal_sign" -> "Sign"
            "eth_sign" -> "Sign"
            "eth_signTypedData" -> "Sign"
            "eth_sendTransaction" -> "Approve"
            "eth_signTransaction" -> "Sign"
            else -> method
        }

        return MethodData(title, shortTitle, evmChain?.name ?: "")
    }

    override fun getAction(
        request: Wallet.Model.SessionRequest.JSONRPCRequest,
        peerMetaData: Core.Model.AppMetaData?,
        chainInternalId: String?,
    ) = when (request.method) {
        else -> throw UnsupportedMethodException(request.method)
    }

    private fun getEvmAddress(account: Account, chain: Chain) =
        when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                val seed: ByteArray = accountType.seed
                Signer.address(seed, chain)
            }

            is AccountType.EvmPrivateKey -> {
                Signer.address(accountType.key)
            }

            is AccountType.EvmAddress -> {
                Address(accountType.address)
            }

            else -> throw UnsupportedAccountException()
        }

    override fun getChainName(chainInternalId: String): String? {
        val evmChainId = chainInternalId.toInt()

        return supportedEvmChains.find { it.id == evmChainId }?.name
    }

}
