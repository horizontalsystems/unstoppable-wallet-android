package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

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
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable

class BackupRequiredDialog : BaseComposableBottomSheetFragment() {

    private val account by lazy {
        requireArguments().parcelable<Account>(ACCOUNT)
    }

    private val text by lazy {
        requireArguments().getString(TEXT) ?: ""
    }

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
                account?.let {
                    BackupRequiredScreen(findNavController(), it, text)
                }
            }
        }
    }

    companion object {
        private const val ACCOUNT = "account"
        private const val TEXT = "text"

        fun prepareParams(account: Account, text: String) = bundleOf(
            ACCOUNT to account,
            TEXT to text
        )
    }
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
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                text = text
            )
            Spacer(Modifier.height(20.dp))
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.BackupRecoveryPhrase_Backup),
                onClick = {
                    navController.popBackStack()
                    navController.slideFromBottom(
                        R.id.backupKeyFragment,
                        BackupKeyModule.prepareParams(account)
                    )
                }
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
