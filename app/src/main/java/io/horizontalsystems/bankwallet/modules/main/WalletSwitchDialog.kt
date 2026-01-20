package io.horizontalsystems.bankwallet.modules.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.WalletSwitchBottomSheet
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.core.findNavController

class WalletSwitchDialog : BaseComposableBottomSheetFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                WalletSwitchScreen(findNavController())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletSwitchScreen(navController: NavController) {
    val accountManager = App.accountManager
    val wallets = accountManager.accounts.filter { !it.isWatchAccount }
    val watchWallets = accountManager.accounts.filter { it.isWatchAccount }
    val activeWallet = accountManager.activeAccount

    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = {
                navController.popBackStack()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            WalletSwitchBottomSheet(
                wallets = wallets,
                watchingAddresses = watchWallets,
                selectedAccount = activeWallet,
                onSelectListener = { account: Account ->
                    accountManager.setActiveAccountId(account.id)
                    navController.popBackStack()

                    stat(
                        page = StatPage.SwitchWallet,
                        event = StatEvent.Select(StatEntity.Wallet)
                    )
                },
                onCancelClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
