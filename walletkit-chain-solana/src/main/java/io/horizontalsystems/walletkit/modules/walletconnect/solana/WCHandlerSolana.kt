package io.horizontalsystems.walletkit.modules.walletconnect.solana

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.managers.SolanaKitManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.modules.walletconnect.handler.IWCHandler
import io.horizontalsystems.walletkit.modules.walletconnect.handler.MethodData
import io.horizontalsystems.walletkit.modules.walletconnect.handler.UnsupportedMethodException
import io.horizontalsystems.walletkit.modules.walletconnect.request.AbstractWCAction
import io.horizontalsystems.dapp.core.HSDAppRequest
import io.horizontalsystems.solanakit.Signer

class WCHandlerSolana(private val solanaKitManager: SolanaKitManager) : IWCHandler {
    override val chainNamespace = WCSolanaHelper.chainNamespace

    override val supportedChains = WCSolanaHelper.supportedChains
    override val supportedMethods = WCSolanaHelper.supportedMethods
    override val supportedEvents = WCSolanaHelper.supportedEvents

    override fun getAction(request: HSDAppRequest, chainInternalId: String?): AbstractWCAction {
        return when (request.method) {
            "solana_signMessage" -> WCActionSolanaSignMessage(
                request.params,
                signer(),
            )

            "solana_signTransaction" -> WCActionSolanaSignTransaction(
                request.params,
                signer(),
                multiple = false,
                peerName = request.peerMetaData?.name,
            )

            "solana_signAllTransactions" -> WCActionSolanaSignTransaction(
                request.params,
                signer(),
                multiple = true,
                peerName = request.peerMetaData?.name,
            )

            "solana_signAndSendTransaction" -> WCActionSolanaSignAndSendTransaction(
                request.params,
                peerName = request.peerMetaData?.name,
            )

            else -> throw UnsupportedMethodException(request.method)
        }
    }

    // The kit signer is null for watch-only (AccountType.SolanaAddress) accounts, which cannot
    // sign. In practice such accounts can't reach here — only Mnemonic/EvmPrivateKey accounts
    // support WalletConnect (Account.supportsWalletConnect) — but guard defensively regardless.
    private fun signer(): Signer {
        val account = App.accountManager.activeAccount ?: throw WCSolanaHelper.NoSignerException()
        return solanaKitManager.getSolanaKitWrapper(account).signer ?: throw WCSolanaHelper.NoSignerException()
    }

    override fun getAccountAddresses(account: Account): List<String> {
        // Called eagerly for every handler when approving a session (see
        // WCManager.getSupportedNamespaces). Account types with no Solana address
        // (e.g. EvmPrivateKey) must contribute nothing rather than throw, otherwise
        // approval would break even for EVM-only dApps.
        val accountType = account.type
        if (accountType !is AccountType.Mnemonic && accountType !is AccountType.SolanaAddress) {
            return emptyList()
        }

        val address = solanaKitManager.getAddress(accountType)
        return supportedChains.map { "$it:$address" }
    }

    override fun getMethodData(method: String, chainInternalId: String?): MethodData {
        val title = when (method) {
            "solana_signMessage" -> "Sign Message"
            "solana_signTransaction",
            "solana_signAllTransactions" -> "Sign Transaction"
            "solana_signAndSendTransaction" -> "Approve Transaction"
            else -> method
        }

        return MethodData(title, "Sign", "Solana")
    }

    override fun getChainName(chainInternalId: String) = "Solana"
}
