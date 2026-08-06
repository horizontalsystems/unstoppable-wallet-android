package io.horizontalsystems.walletkit.modules.multiswap.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.adapters.StellarAssetAdapter
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.activatetoken.ActivateTokenPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The Stellar trustline pre-swap step (the EIP-20 approve analogue): buying a classic
// asset requires the account's trustline BEFORE the swap — the server's preflight (and
// the chain itself, op_no_trust) rejects it otherwise. Reuses the same ActivateTokenPage
// the Receive flow presents, so there is one changeTrust path, not two.
class ActionActivateStellarAsset(
    private val wallet: Wallet,
    override val inProgress: Boolean = false,
) : ISwapProviderAction {

    @Composable
    override fun getTitle() = stringResource(R.string.Button_Activate)

    @Composable
    override fun getTitleInProgress() = stringResource(R.string.Activate_Activating)

    @Composable
    override fun executor(navigation: HSNavigation, onActionCompleted: () -> Unit): () -> Unit {
        val runActivation = navigation.slideFromBottomForResult<ActivateTokenPage.Result>(
            { ActivateTokenPage(wallet) }
        ) {
            if (it.activated) {
                onActionCompleted.invoke()
            }
        }

        val view = LocalView.current
        val scope = rememberCoroutineScope()
        return {
            scope.launch {
                // ActivateTokenPage resolves its adapter from the wallet synchronously, so
                // navigating without one would land on a broken page — surface an error
                // instead and let the user tap again (the adapter rebuild keeps running).
                if (ensureAdapterReady()) {
                    runActivation()
                } else {
                    HudHelper.showErrorMessage(view, R.string.Error)
                }
            }
        }
    }

    // Swapping INTO a token the user never added: the trustline check found no wallet, so
    // enable it first and wait out the async adapter rebuild. True when the adapter is
    // available, false when it did not appear within the polling window.
    private suspend fun ensureAdapterReady(): Boolean {
        if (adapterReady()) return true

        if (!App.walletManager.activeWallets.contains(wallet)) {
            App.walletActivator.activateTokens(wallet.account, listOf(wallet.token))
        }

        repeat(20) {
            if (adapterReady()) return true
            delay(100)
        }

        return false
    }

    private fun adapterReady() =
        App.adapterManager.getAdapterForWallet<StellarAssetAdapter>(wallet) != null
}
