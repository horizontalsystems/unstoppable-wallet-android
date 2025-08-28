package io.horizontalsystems.bankwallet.modules.manageaccount.showmonerokey

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.MenuItem
import io.horizontalsystems.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyModule.MoneroKeyType
import io.horizontalsystems.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyModule.MoneroKeys
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.ActionButton
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.ConfirmCopyBottomSheet
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.HidableContent
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.SelectorDialogCompose
import io.horizontalsystems.bankwallet.ui.compose.components.SelectorItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class ShowMoneroKeyFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()
        val keys = input?.keys

        if (keys == null) {
            NoKeysScreen()
        } else {
            ShowMoneroKeyScreen(navController, keys)
        }
    }

    @Parcelize
    data class Input(val keys: MoneroKeys) : Parcelable
}

@Composable
private fun ShowMoneroKeyScreen(
    navController: NavController,
    keys: MoneroKeys
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    var keyType by remember { mutableStateOf(MoneroKeyType.Spend) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            ConfirmCopyBottomSheet(
                onConfirm = {
                    coroutineScope.launch {
                        TextHelper.copyText(keys.getKey(keyType))
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                        sheetState.hide()
                    }
                },
                onCancel = {
                    coroutineScope.launch {
                        sheetState.hide()
                    }
                }
            )
        }
    ) {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = stringResource(keys.title),
                    navigationIcon = {
                        HsBackButton(onClick = navController::popBackStack)
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top
                ) {
                    VSpacer(12.dp)

                    if (keys.isPrivate) {
                        TextImportantWarning(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = stringResource(R.string.PrivateKeys_NeverShareWarning)
                        )
                        VSpacer(24.dp)
                    }

                    var showKeyTypeSelectorDialog by remember { mutableStateOf(false) }

                    CellUniversalLawrenceSection {
                        MenuItem(
                            title = stringResource(R.string.MoneroKeyType),
                            value = stringResource(keyType.title),
                            onClick = { showKeyTypeSelectorDialog = true }
                        )
                    }

                    VSpacer(32.dp)
                    if (keys.isPrivate) {
                        HidableContent(
                            keys.getKey(keyType),
                            stringResource(R.string.ExtendedKey_TapToShowPrivateKey)
                        )
                    } else {
                        HidableContent(keys.getKey(keyType))
                    }

                    if (showKeyTypeSelectorDialog) {
                        SelectorDialogCompose(
                            title = stringResource(R.string.MoneroKeyType),
                            items = MoneroKeyType.entries.map {
                                SelectorItem(stringResource(it.title), it == keyType, it)
                            },
                            onDismissRequest = {
                                showKeyTypeSelectorDialog = false
                            },
                            onSelectItem = {
                                keyType = it
                            }
                        )
                    }
                }

                ActionButton(R.string.Alert_Copy) {
                    if (keys.isPrivate) {
                        coroutineScope.launch {
                            sheetState.show()
                        }
                    } else {
                        TextHelper.copyText(keys.getKey(keyType))
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                    }
                }
            }
        }
    }
}


@Composable
private fun NoKeysScreen() {

}
