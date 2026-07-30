package io.horizontalsystems.walletkit.modules.multiswap.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.adapters.StellarAssetAdapter
import io.horizontalsystems.walletkit.entities.Wallet
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

        val scope = rememberCoroutineScope()
        return {
            scope.launch {
                ensureWalletEnabled()
                runActivation()
            }
        }
    }

    // Swapping INTO a token the user never added: the trustline check found no wallet, but
    // ActivateTokenPage resolves its adapter from the wallet synchronously — so enable the
    // wallet first and wait out the async adapter rebuild before navigating.
    private suspend fun ensureWalletEnabled() {
        if (App.walletManager.activeWallets.contains(wallet)) return

        App.walletActivator.activateTokens(wallet.account, listOf(wallet.token))

        repeat(20) {
            if (App.adapterManager.getAdapterForWallet<StellarAssetAdapter>(wallet) != null) return
            delay(100)
        }
    }
}
