package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
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
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefaultWithIcon
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithIcon
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import kotlinx.parcelize.Parcelize

class BackupRequiredDialog : BaseComposableBottomSheetFragment() {

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
                val navController = findNavController()
                navController.getInput<Input>()?.let { input ->
                    BackupRequiredScreen(navController, input.account, input.text)
                }
            }
        }
    }

    @Parcelize
    data class Input(val account: Account, val text: String) : Parcelable
}

@Composable
fun BackupRequiredScreen(navController: NavController, account: Account, text: String) {
    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_attention_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            title = stringResource(R.string.ManageAccount_BackupRequired_Title),
            onCloseClick = {
                navController.popBackStack()
            }
        ) {
            VSpacer(12.dp)
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = text
            )
            VSpacer(32.dp)
            ButtonPrimaryYellowWithIcon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.BackupRecoveryPhrase_ManualBackup),
                icon = R.drawable.ic_edit_24,
                iconTint = ComposeAppTheme.colors.dark,
                onClick = {
                    navController.slideFromBottom(
                        R.id.backupKeyFragment,
                        account
                    )

                    stat(page = StatPage.BackupRequired, event = StatEvent.Open(StatPage.ManualBackup))
                }
            )
            VSpacer(12.dp)
            ButtonPrimaryDefaultWithIcon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.BackupRecoveryPhrase_LocalBackup),
                icon = R.drawable.ic_file_24,
                iconTint = ComposeAppTheme.colors.claude,
                onClick = {
                    navController.slideFromBottom(R.id.backupLocalFragment, account)

                    stat(page = StatPage.BackupRequired, event = StatEvent.Open(StatPage.FileBackup))
                }
            )
            VSpacer(12.dp)
            ButtonPrimaryTransparent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.BackupRecoveryPhrase_Later),
                onClick = {
                    navController.popBackStack()
                }
            )
            VSpacer(32.dp)
        }
    }
}
