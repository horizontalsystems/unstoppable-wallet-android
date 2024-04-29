package io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyModule.DisplayKeyType
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.ActionButton
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.ConfirmCopyBottomSheet
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.HidableContent
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SelectorDialogCompose
import io.horizontalsystems.bankwallet.ui.compose.components.SelectorItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class ShowExtendedKeyFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()
        val hdExtendedKey = input?.extendedRootKey
        val displayKeyType = input?.displayKeyType

        if (hdExtendedKey == null || displayKeyType == null) {
            NoExtendKeyScreen()
        } else {
            ShowExtendedKeyScreen(
                navController,
                hdExtendedKey,
                displayKeyType
            )
        }
    }

    @Parcelize
    data class Input(val extendedRootKeySerialized: String, val displayKeyType: DisplayKeyType) : Parcelable {
        val extendedRootKey: HDExtendedKey?
            get() = try {
                HDExtendedKey(extendedRootKeySerialized)
            } catch (error: Throwable) {
                null
            }

        constructor(extendedRootKey: HDExtendedKey, displayKeyType: DisplayKeyType) : this(extendedRootKey.serialize(), displayKeyType)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ShowExtendedKeyScreen(
    navController: NavController,
    extendedKey: HDExtendedKey,
    displayKeyType: DisplayKeyType
) {
    val viewModel = viewModel<ShowExtendedKeyViewModel>(factory = ShowExtendedKeyModule.Factory(extendedKey, displayKeyType))

    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            ConfirmCopyBottomSheet(
                onConfirm = {
                    coroutineScope.launch {
                        TextHelper.copyText(viewModel.extendedKey)
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                        sheetState.hide()

                        viewModel.logEvent(StatEvent.Copy(StatEntity.Key))
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
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = viewModel.title.getString(),
                navigationIcon = {
                    HsBackButton(onClick = navController::popBackStack)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Info_Title),
                        icon = R.drawable.ic_info_24,
                        onClick = {
                            FaqManager.showFaqPage(navController, FaqManager.faqPathPrivateKeys)

                            viewModel.logEvent(StatEvent.Open(StatPage.Info))
                        }
                    )
                )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(Modifier.height(12.dp))

                if (viewModel.displayKeyType.isPrivate) {
                    TextImportantWarning(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.PrivateKeys_NeverShareWarning)
                    )
                    Spacer(Modifier.height(24.dp))
                }

                var showBlockchainSelectorDialog by remember { mutableStateOf(false) }
                var showPurposeSelectorDialog by remember { mutableStateOf(false) }
                var showAccountSelectorDialog by remember { mutableStateOf(false) }

                val menuItems = buildList<@Composable () -> Unit> {
                    add {
                        MenuItem(
                            title = stringResource(R.string.ExtendedKey_Purpose),
                            value = viewModel.purpose.name,
                            onClick = if (viewModel.displayKeyType == DisplayKeyType.Bip32RootKey || viewModel.displayKeyType.isDerivable) {
                                { showPurposeSelectorDialog = true }
                            } else {
                                null
                            }
                        )
                    }
                    if (viewModel.displayKeyType.isDerivable) {
                        add {
                            MenuItem(
                                title = stringResource(R.string.ExtendedKey_Blockchain),
                                value = viewModel.blockchain.name,
                                onClick = { showBlockchainSelectorDialog = true }
                            )
                        }
                        add {
                            MenuItem(
                                title = stringResource(R.string.ExtendedKey_Account),
                                value = viewModel.account.toString(),
                                onClick = { showAccountSelectorDialog = true }
                            )
                        }
                    }
                }

                if (menuItems.isNotEmpty()) {
                    CellUniversalLawrenceSection(menuItems)
                }

                Spacer(Modifier.height(32.dp))
                if (viewModel.displayKeyType.isPrivate) {
                    HidableContent(viewModel.extendedKey, stringResource(R.string.ExtendedKey_TapToShowPrivateKey)) {
                        viewModel.logEvent(StatEvent.ToggleHidden)
                    }
                } else {
                    HidableContent(viewModel.extendedKey)
                }

                if (showPurposeSelectorDialog) {
                    SelectorDialogCompose(
                        title = stringResource(R.string.ExtendedKey_Purpose),
                        items = viewModel.purposes.map {
                            SelectorItem(it.name, it == viewModel.purpose, it)
                        },
                        onDismissRequest = {
                            showPurposeSelectorDialog = false
                        },
                        onSelectItem = {
                            viewModel.set(it)
                        }
                    )
                }
                if (showBlockchainSelectorDialog) {
                    SelectorDialogCompose(
                        title = stringResource(R.string.ExtendedKey_Blockchain),
                        items = viewModel.blockchains.map {
                            SelectorItem(it.name, it == viewModel.blockchain, it)
                        },
                        onDismissRequest = {
                            showBlockchainSelectorDialog = false
                        },
                        onSelectItem = {
                            viewModel.set(it)
                        }
                    )
                }
                if (showAccountSelectorDialog) {
                    SelectorDialogCompose(
                        title = stringResource(R.string.ExtendedKey_Account),
                        items = viewModel.accounts.map {
                            SelectorItem(it.toString(), it == viewModel.account, it)
                        },
                        onDismissRequest = {
                            showAccountSelectorDialog = false
                        },
                        onSelectItem = {
                            viewModel.set(it)
                        }
                    )
                }
            }
            ActionButton(R.string.Alert_Copy) {
                if (viewModel.displayKeyType.isPrivate) {
                    coroutineScope.launch {
                        sheetState.show()
                    }
                } else {
                    TextHelper.copyText(viewModel.extendedKey)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)

                    viewModel.logEvent(StatEvent.Copy(StatEntity.Key))
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    title: String,
    value: String,
    onClick: (() -> Unit)?
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        body_leah(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))

        Row(
            Modifier
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            subhead1_grey(
                text = value,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            if (onClick != null) {
                Icon(
                    modifier = Modifier.padding(start = 4.dp),
                    painter = painterResource(id = R.drawable.ic_down_arrow_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}

@Composable
private fun NoExtendKeyScreen() {

}
