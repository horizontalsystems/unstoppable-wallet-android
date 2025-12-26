package io.horizontalsystems.bankwallet.modules.walletconnect

import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.handler.IWCHandler
import io.horizontalsystems.bankwallet.modules.walletconnect.handler.MethodData
import io.horizontalsystems.bankwallet.modules.walletconnect.request.AbstractWCAction
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCChainData
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ValidationError
import io.horizontalsystems.marketkit.models.BlockchainType


class WCManager(
    private val accountManager: IAccountManager,
) {
    sealed class SupportState {
        object Supported : SupportState()
        object NotSupportedDueToNoActiveAccount : SupportState()
        class NotSupportedDueToNonBackedUpAccount(val account: Account) : SupportState()
        class NotSupported(val accountTypeDescription: String) : SupportState()
    }

    private val handlersMap = mutableMapOf<String, IWCHandler>()

    fun addWcHandler(wcHandler: IWCHandler) {
        handlersMap[wcHandler.chainNamespace] = wcHandler
    }

    fun getMethodData(sessionRequest: Wallet.Model.SessionRequest): MethodData? {
        val chainId = sessionRequest.chainId ?: return null
        val chainParts = chainId.split(":")

        val chainNamespace = chainParts.getOrNull(0)
        val chainInternalId = chainParts.getOrNull(1)

        val handler = handlersMap[chainNamespace] ?: return null

        return handler.getMethodData(sessionRequest.request.method, chainInternalId)
    }

    fun getActionForRequest(sessionRequest: Wallet.Model.SessionRequest?): AbstractWCAction? {
        if (sessionRequest == null) return null
        val chainId = sessionRequest.chainId ?: return null
        val chainParts = chainId.split(":")

        val chainNamespace = chainParts.getOrNull(0)
        val chainInternalId = chainParts.getOrNull(1)

        val handler = handlersMap[chainNamespace] ?: return null

        return handler.getAction(sessionRequest.request, sessionRequest.peerMetaData, chainInternalId)
    }

    fun getWalletConnectSupportState(): SupportState {
        val tmpAccount = accountManager.activeAccount
        return when {
            tmpAccount == null -> SupportState.NotSupportedDueToNoActiveAccount
            !tmpAccount.isBackedUp && !tmpAccount.isFileBackedUp -> SupportState.NotSupportedDueToNonBackedUpAccount(
                tmpAccount
            )
            tmpAccount.type.supportsWalletConnect -> SupportState.Supported
            else -> SupportState.NotSupported(tmpAccount.type.description)
        }
    }

    fun getBlockchainType(sessionChainId: String?): BlockchainType? {
        val chainId = getChainData(sessionChainId)?.id
        return chainId?.let { App.evmBlockchainManager.getBlockchain(it) }?.type
    }

    fun getChainData(chainId: String?): WCChainData? {
        return WCUtils.getChainData(chainId ?: return null)
    }

    fun validate(requiredNamespaces: Map<String, Wallet.Model.Namespace.Proposal>) {
        requiredNamespaces.forEach { (chainNamespace, proposal) ->
            val handler = handlersMap[chainNamespace]
                ?: throw ValidationError.UnsupportedChainNamespace(chainNamespace)

            proposal.chains?.let { requiredChains ->
                val unsupportedChains = requiredChains - handler.supportedChains
                if (unsupportedChains.isNotEmpty()) {
                    throw ValidationError.UnsupportedChains(unsupportedChains)
                }
            }

            val unsupportedMethods = proposal.methods - handler.supportedMethods
            if (unsupportedMethods.isNotEmpty()) {
                throw ValidationError.UnsupportedMethods(unsupportedMethods)
            }

            val unsupportedEvents = proposal.events - handler.supportedEvents
            if (unsupportedEvents.isNotEmpty()) {
                throw ValidationError.UnsupportedEvents(unsupportedEvents)
            }
        }
    }

    fun getSupportedNamespaces(account: Account) =
        handlersMap.map { (chainNamespace, handler) ->
            chainNamespace to Wallet.Model.Namespace.Session(
                chains = handler.supportedChains,
                methods = handler.supportedMethods,
                events = handler.supportedEvents,
                accounts = handler.getAccountAddresses(account)
            )
        }.toMap()

    fun getChainNames(namespaces: Map<String, Wallet.Model.Namespace.Session>): List<String> {
        val res = mutableListOf<String>()

        for ((chainNamespace, session) in namespaces) {
            val handler = handlersMap[chainNamespace] ?: continue

            for (accountId in session.accounts) {
                val accountIdParts = accountId.split(":")
                val chainInternalId = accountIdParts.getOrNull(1) ?: continue

                handler.getChainName(chainInternalId)?.let {
                    res.add(it)
                }
            }
        }

        return res
    }
}
