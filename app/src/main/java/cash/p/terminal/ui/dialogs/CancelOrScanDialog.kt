package cash.p.terminal.ui.dialogs

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.navigation.popBackStackSafely
import cash.p.terminal.navigation.setNavigationResultX
import cash.p.terminal.ui_compose.components.InfoTextBody
import cash.p.terminal.ui_compose.BaseComposableBottomSheetFragment
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.findNavController
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IAccountManager
import kotlinx.parcelize.Parcelize
import org.koin.android.ext.android.inject

class CancelOrScanDialog : BaseComposableBottomSheetFragment() {

    private val accountManager: IAccountManager by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val accountType = accountManager.activeAccount?.type
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                CancelOrScanScreen(findNavController(), accountType)
            }
        }
    }

    @Parcelize
    data class Result(val confirmed: Boolean) : Parcelable
}

@Composable
private fun CancelOrScanScreen(navController: NavController, accountType: AccountType?) {
    val isTrezor = accountType is AccountType.TrezorDevice
    val bodyText = stringResource(
        if (isTrezor) R.string.connect_trezor_to_add_message else R.string.scan_to_add_message
    )
    val confirmText = stringResource(
        if (isTrezor) R.string.connect else R.string.scan
    )

    ComposeAppTheme {
        BottomSheetHeader(
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.lucian),
            title = stringResource(R.string.adding_tokens),
            onCloseClick = {
                navController.popBackStackSafely()
            }
        ) {
            InfoTextBody(text = bodyText)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .weight(1f),
                    title = confirmText,
                    onClick = {
                        navController.setNavigationResultX(CancelOrScanDialog.Result(true))
                        navController.popBackStackSafely()
                    }
                )
                ButtonPrimaryDefault(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.common_cancel),
                    onClick = {
                        navController.setNavigationResultX(CancelOrScanDialog.Result(false))
                        navController.popBackStackSafely()
                    }
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
