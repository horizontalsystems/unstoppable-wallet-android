package io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyModule.DisplayKeyType
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.ActionButton
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.ConfirmCopyBottomSheet
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.HidableContent
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.parcelable
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import kotlinx.coroutines.launch

class ShowExtendedKeyFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent() {
        val hdExtendedKey = arguments?.getString(ShowExtendedKeyModule.EXTENDED_ROOT_KEY)?.let {
            try {
                HDExtendedKey(it)
            } catch (error: Throwable) {
                null
            }
        }
        val displayKeyType = arguments?.parcelable<DisplayKeyType>(ShowExtendedKeyModule.DISPLAY_KEY_TYPE)

        if (hdExtendedKey == null || displayKeyType == null) {
            NoExtendKeyScreen()
        } else {
            ShowExtendedKeyScreen(
                findNavController(),
                hdExtendedKey,
                displayKeyType
            )
        }
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

    ComposeAppTheme {
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
                    title = viewModel.title,
                    navigationIcon = {
                        HsBackButton(onClick = navController::popBackStack)
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Info_Title),
                            icon = R.drawable.ic_info_24,
                            onClick = {
                                FaqManager.showFaqPage(navController, FaqManager.faqPathPrivateKeys)
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

                    if(viewModel.displayKeyType.isPrivate) {
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
                        HidableContent(viewModel.extendedKey, stringResource(R.string.ExtendedKey_TapToShowPrivateKey))
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
                    }
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
