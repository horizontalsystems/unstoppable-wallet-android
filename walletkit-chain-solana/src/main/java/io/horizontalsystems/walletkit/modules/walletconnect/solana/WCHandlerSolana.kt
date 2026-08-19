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
                expectedPubkey = activeSolanaAddress(),
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

    // Only Mnemonic accounts can sign for Solana. Watch-only (SolanaAddress) accounts have no key,
    // and other types (e.g. an EvmPrivateKey active account switched to after this session was
    // approved) have no Solana key at all — for those, SolanaKitManager.getSolanaKitWrapper would
    // throw UnsupportedAccountException. Guard the type up front so we neither spin up the kit for a
    // non-signing account nor leak that exception; a NoSignerException here is caught by
    // WCManager.getActionForRequest and surfaced as an error screen.
    private fun signer(): Signer {
        val account = App.accountManager.activeAccount ?: throw WCSolanaHelper.NoSignerException()
        if (account.type !is AccountType.Mnemonic) throw WCSolanaHelper.NoSignerException()
        return solanaKitManager.getSolanaKitWrapper(account).signer ?: throw WCSolanaHelper.NoSignerException()
    }

    // Base58 address of the active account, so solana_signMessage can reject a request whose
    // `pubkey` names a different account. Best-effort: returns null (skip the check) rather than
    // throw, since signer() has already established the account can sign by the time this runs.
    private fun activeSolanaAddress(): String? {
        val accountType = App.accountManager.activeAccount?.type ?: return null
        return try {
            solanaKitManager.getAddress(accountType)
        } catch (e: Exception) {
            null
        }
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
