package io.horizontalsystems.bankwallet.modules.walletconnect.pairing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryRed
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.core.setNavigationResult

class ConfirmDeleteAllPairingsDialog : BaseComposableBottomSheetFragment() {

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
                ConfirmDeleteAllScreen(findNavController())
            }
        }
    }

    companion object {
        private const val resultKey = "ConfirmDeleteAllPairingsDialogResultKey"

        fun onConfirm(navController: NavController, callback: () -> Unit) {
            navController.getNavigationResult(resultKey) {
                if (it.getBoolean(resultKey)) {
                    callback.invoke()
                }
            }
        }

        fun setResult(navController: NavController, confirmed: Boolean) {
            navController.setNavigationResult(resultKey, bundleOf(resultKey to confirmed))
        }
    }
}

@Composable
fun ConfirmDeleteAllScreen(navController: NavController) {
    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_delete_20),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.lucian),
            title = stringResource(R.string.WalletConnect_DeleteAllPairs),
            onCloseClick = {
                navController.popBackStack()
            }
        ) {
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                text = stringResource(R.string.WalletConnect_Pairings_ConfirmationDeleteAll)
            )
            Spacer(Modifier.height(20.dp))
            ButtonPrimaryRed(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.WalletConnect_Pairings_Delete),
                onClick = {
                    ConfirmDeleteAllPairingsDialog.setResult(navController, true)
                    navController.popBackStack()
                }
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
