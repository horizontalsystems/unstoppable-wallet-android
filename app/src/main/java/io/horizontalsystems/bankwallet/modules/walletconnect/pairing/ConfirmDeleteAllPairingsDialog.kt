package io.horizontalsystems.bankwallet.modules.walletconnect.pairing

import android.os.Bundle
import android.os.Parcelable
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
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryRed
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import kotlinx.parcelize.Parcelize

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

    @Parcelize
    data class Result(val confirmed: Boolean) : Parcelable
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
                    navController.setNavigationResultX(ConfirmDeleteAllPairingsDialog.Result(true))
                    navController.popBackStack()
                }
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
