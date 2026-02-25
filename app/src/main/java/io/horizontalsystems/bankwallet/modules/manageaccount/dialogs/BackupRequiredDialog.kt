package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.nav3.BottomSheetSceneStrategy
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.core.findNavController
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

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
//                    BackupRequiredScreen(navController, input.account, input.text)
                }
            }
        }
    }

    @Parcelize
    data class Input(val account: Account, val text: String) : Parcelable
}

@Serializable
data class BackupRequiredScreen(val account: Account, val text: String) : HSScreen() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun getMetadata() = BottomSheetSceneStrategy.bottomSheet()

    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        BackupRequiredScreen(backStack, account, text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRequiredScreen(backStack: NavBackStack<HSScreen>, account: Account, text: String) {
    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = backStack::removeLastOrNull,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            buttons = {
                HSButton(
                    title = stringResource(R.string.BackupRecoveryPhrase_ManualBackup),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
//                        TODO("xxx nav3")
//                        navController.slideFromBottom(
//                            R.id.backupKeyFragment,
//                            account
//                        )
//
//                        stat(
//                            page = StatPage.BackupRequired,
//                            event = StatEvent.Open(StatPage.ManualBackup)
//                        )
                    }
                )
                HSButton(
                    title = stringResource(R.string.BackupRecoveryPhrase_LocalBackup),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    onClick = {
//                        TODO("xxx nav3")
//                        navController.slideFromBottom(R.id.backupLocalFragment, account)
//
//                        stat(
//                            page = StatPage.BackupRequired,
//                            event = StatEvent.Open(StatPage.FileBackup)
//                        )
                    }
                )
                HSButton(
                    title = stringResource(R.string.BackupRecoveryPhrase_Later),
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Transparent,
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    onClick = backStack::removeLastOrNull
                )
            },
            content = {
                BottomSheetHeaderV3(
                    image72 = painterResource(R.drawable.warning_filled_24),
                    imageTint = ComposeAppTheme.colors.jacob,
                    title = stringResource(R.string.BackupManager_BackupRequired)
                )
                TextBlock(text = text, textAlign = TextAlign.Center)
            }
        )
    }
}
