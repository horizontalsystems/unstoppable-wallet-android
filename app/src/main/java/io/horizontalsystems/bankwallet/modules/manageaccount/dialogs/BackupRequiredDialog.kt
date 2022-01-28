package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backupkey.BackupKeyModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportant
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController

class BackupRequiredDialog : BaseComposableBottomSheetFragment() {

    private val account by lazy {
        requireArguments().getParcelable<Account>(ACCOUNT)
    }

    private val coinTitle by lazy {
        requireArguments().getString(COIN_TITLE) ?: ""
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
                    BackupRequiredScreen(findNavController(), it, coinTitle)
                }
            }
        }
    }

    companion object {
        private const val ACCOUNT = "account"
        private const val COIN_TITLE = "coin_title"

        fun prepareParams(account: Account, coinTitle: String) = bundleOf(
            ACCOUNT to account,
            COIN_TITLE to coinTitle,
        )
    }
}

@Composable
fun BackupRequiredScreen(navController: NavController, account: Account, coinTitle: String) {
    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_attention_red_24),
            title = stringResource(R.string.ManageAccount_BackupRequired_Title),
            subtitle = account.name,
            onCloseClick = {
                navController.popBackStack()
            }
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            TextImportant(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                text = stringResource(R.string.ManageAccount_BackupRequired_Description, account.name, coinTitle)
            )
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = stringResource(R.string.ManageAccount_RecoveryPhraseBackup),
                onClick = {
                    navController.popBackStack()
                    navController.slideFromRight(
                        R.id.mainFragment_to_backupKeyFragment,
                        BackupKeyModule.prepareParams(account)
                    )
                }
            )
        }
    }
}
