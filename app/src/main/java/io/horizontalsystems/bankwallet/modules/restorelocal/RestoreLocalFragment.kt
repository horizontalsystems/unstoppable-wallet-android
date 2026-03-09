package io.horizontalsystems.bankwallet.modules.restorelocal

import android.os.Parcelable
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputPassword
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class RestoreLocalScreen(
    val popOffOnSuccess: KClass<out HSScreen>,
    val popOffInclusive: Boolean,
    val jsonFile: String,
    val fileName: String?,
    val statPage: StatPage
) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val mainViewModel = viewModel<RestoreViewModel>()
        val viewModel = viewModel<RestoreLocalViewModel>(
            factory = RestoreLocalModule.Factory(
                jsonFile,
                fileName,
                statPage
            )
        )

        RestoreLocalScreen(
            viewModel = viewModel,
            mainViewModel = mainViewModel,
            statPage = statPage,
            onBackClick = { backStack.removeLastOrNull() },
            close = {
                backStack.removeLastUntil(popOffOnSuccess, popOffInclusive)
            },
            openSelectCoins = {
                backStack.add(RestoreLocalSelectCoins(popOffOnSuccess, popOffInclusive))
            },
            openBackupItems = { backStack.add(RestoreLocalBackupFile) }
        )

    }
}

class RestoreLocalFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
    }

    @Parcelize
    data class Input(
        val popOffOnSuccess: Int,
        val popOffInclusive: Boolean,
        val jsonFile: String,
        val fileName: String?,
        val statPage: StatPage
    ) : Parcelable
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RestoreLocalScreen(
    viewModel: RestoreLocalViewModel,
    mainViewModel: RestoreViewModel,
    statPage: StatPage,
    onBackClick: () -> Unit,
    close: () -> Unit,
    openSelectCoins: () -> Unit,
    openBackupItems: () -> Unit
) {
    val uiState = viewModel.uiState
    var hidePassphrase by remember { mutableStateOf(true) }
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.restored) {
        if (uiState.restored) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_Restored,
                icon = R.drawable.icon_add_to_wallet_2_24,
                iconTint = R.color.white
            )
            delay(300)
            close.invoke()
        }
    }

    LaunchedEffect(uiState.parseError) {
        uiState.parseError?.let { error ->
            Toast.makeText(
                App.instance,
                error.message ?: error.javaClass.simpleName,
                Toast.LENGTH_LONG
            ).show()
            onBackClick.invoke()
        }
    }

    LaunchedEffect(uiState.showSelectCoins) {
        uiState.showSelectCoins?.let { accountType ->
            mainViewModel.setAccountData(
                accountType,
                viewModel.accountName,
                uiState.manualBackup,
                true,
                statPage
            )
            keyboardController?.hide()
            delay(300)
            openSelectCoins.invoke()
            viewModel.onSelectCoinsShown()

            stat(page = statPage, event = StatEvent.Open(StatPage.RestoreSelect))
        }
    }

    LaunchedEffect(uiState.showBackupItems) {
        if (uiState.showBackupItems) {
            keyboardController?.hide()
            delay(300)
            openBackupItems.invoke()
            viewModel.onBackupItemsShown()
        }
    }

    HSScaffold(
        title = stringResource(R.string.ImportBackupFile_EnterPassword),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = onBackClick
            )
        )
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                InfoText(text = stringResource(R.string.ImportBackupFile_EnterPassword_Description))
                VSpacer(24.dp)
                FormsInputPassword(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hint = stringResource(R.string.ImportBackupFile_BackupPassword),
                    state = uiState.passphraseState,
                    onValueChange = viewModel::onChangePassphrase,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    hide = hidePassphrase,
                    onToggleHide = {
                        hidePassphrase = !hidePassphrase
                    }
                )
                VSpacer(32.dp)
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellowWithSpinner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Restore),
                    showSpinner = uiState.showButtonSpinner,
                    enabled = uiState.showButtonSpinner.not(),
                    onClick = {
                        viewModel.onImportClick()
                    },
                )
            }
        }
    }
}

