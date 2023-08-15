package io.horizontalsystems.bankwallet.modules.backupalert

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonColors
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backuplocal.BackupLocalFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.backupkey.BackupKeyModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimary
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefaults
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable

class BackupRecoveryPhraseDialog : BaseComposableBottomSheetFragment() {

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
                val account = requireArguments().parcelable<Account>(accountKey)!!
                BackupRecoveryPhraseScreen(findNavController(), account)
            }
        }
    }

    companion object {
        private const val accountKey = "accountKey"

        fun prepareParams(account: Account) = bundleOf(accountKey to account)
    }
}

@Composable
fun BackupRecoveryPhraseScreen(navController: NavController, account: Account) {
    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_attention_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            title = stringResource(R.string.BackupRecoveryPhrase_Title),
            onCloseClick = {
                navController.popBackStack()
            }
        ) {
            VSpacer(12.dp)
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.BackupRecoveryPhrase_Description)
            )

            VSpacer(32.dp)
            PrimaryButtonWithIcon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.BackupRecoveryPhrase_ManualBackup),
                icon = R.drawable.ic_edit_24,
                iconTint = ComposeAppTheme.colors.dark,
                buttonColors = ButtonPrimaryDefaults.textButtonColors(
                    backgroundColor = ComposeAppTheme.colors.yellowD,
                    contentColor = ComposeAppTheme.colors.dark,
                    disabledBackgroundColor = ComposeAppTheme.colors.steel20,
                    disabledContentColor = ComposeAppTheme.colors.grey50,
                ),
                onClick = {
                    navController.slideFromBottom(
                        R.id.backupKeyFragment,
                        BackupKeyModule.prepareParams(account)
                    )
                }
            )
            VSpacer(12.dp)
            PrimaryButtonWithIcon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.BackupRecoveryPhrase_LocalBackup),
                icon = R.drawable.ic_file_24,
                iconTint = ComposeAppTheme.colors.claude,
                buttonColors = ButtonPrimaryDefaults.textButtonColors(
                    backgroundColor = ComposeAppTheme.colors.leah,
                    contentColor = ComposeAppTheme.colors.claude,
                    disabledBackgroundColor = ComposeAppTheme.colors.steel20,
                    disabledContentColor = ComposeAppTheme.colors.grey50,
                ),
                onClick = {
                    navController.slideFromBottom(
                        R.id.backupLocalFragment,
                        BackupLocalFragment.prepareParams(account.id)
                    )
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

@Composable
private fun PrimaryButtonWithIcon(
    modifier: Modifier,
    title: String,
    icon: Int,
    iconTint: Color,
    buttonColors: ButtonColors,
    onClick: () -> Unit,
) {
    ButtonPrimary(
        modifier = modifier,
        onClick = onClick,
        buttonColors = buttonColors,
        content = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(icon),
                    tint = iconTint,
                    contentDescription = null
                )
                HSpacer(8.dp)
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
    )
}
