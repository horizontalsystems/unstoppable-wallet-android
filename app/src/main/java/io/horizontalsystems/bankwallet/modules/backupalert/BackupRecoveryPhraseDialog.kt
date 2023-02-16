package io.horizontalsystems.bankwallet.modules.backupalert

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
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.manageaccount.backupkey.BackupKeyModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController

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
                val account = requireArguments().getParcelable<Account>(accountKey)!!
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
            Spacer(modifier = Modifier.height(12.dp))
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.BackupRecoveryPhrase_Description)
            )

            Spacer(modifier = Modifier.height(32.dp))
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.BackupRecoveryPhrase_Backup),
                onClick = {
                    navController.slideFromBottom(
                        R.id.backupKeyFragment,
                        BackupKeyModule.prepareParams(account)
                    )
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ButtonPrimaryTransparent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.BackupRecoveryPhrase_Later),
                onClick = {
                    navController.popBackStack()
                }
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
