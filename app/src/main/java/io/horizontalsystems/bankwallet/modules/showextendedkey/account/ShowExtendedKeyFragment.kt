package io.horizontalsystems.bankwallet.modules.showextendedkey.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.evmprivatekey.ActionButton
import io.horizontalsystems.bankwallet.modules.evmprivatekey.HidableContent
import io.horizontalsystems.bankwallet.modules.recoveryphrase.ConfirmCopyBottomSheet
import io.horizontalsystems.bankwallet.modules.showextendedkey.account.ShowExtendedKeyModule.DisplayKeyType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import kotlinx.coroutines.launch

class AccountExtendedKeyFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        disallowScreenshot()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                val hdExtendedKey = arguments?.getString(ShowExtendedKeyModule.EXTENDED_ROOT_KEY)?.let {
                    try {
                        HDExtendedKey(it)
                    } catch (error: Throwable) {
                        null
                    }
                }
                val displayKeyType = arguments?.getParcelable<DisplayKeyType>(ShowExtendedKeyModule.DISPLAY_KEY_TYPE)

                if (hdExtendedKey == null || displayKeyType == null) {
                    NoExtendKeyScreen()
                } else {
                    AccountExtendedKeyScreen(findNavController(), hdExtendedKey, displayKeyType)
                }
            }
        }
    }

    override fun onDestroyView() {
        allowScreenshot()
        super.onDestroyView()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AccountExtendedKeyScreen(
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
                            TextHelper.copyText(viewModel.accountExtendedKey)
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
                        HsIconButton(onClick = navController::popBackStack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "back button",
                                tint = ComposeAppTheme.colors.jacob
                            )
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.height(12.dp))

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
                        CellSingleLineLawrenceSection(menuItems)
                    }

                    Spacer(Modifier.height(32.dp))
                    var hidden by remember { mutableStateOf(true) }
                    if (viewModel.displayKeyType.isPrivate) {
                        HidableContent(viewModel.accountExtendedKey, hidden, viewModel.showKeyTitle.getString()) {
                            hidden = it
                        }
                    } else {
                        HidableContent(viewModel.accountExtendedKey, false, viewModel.showKeyTitle.getString(), null)
                    }

                    if (showPurposeSelectorDialog) {
                        SelectorDialogCompose(
                            title = stringResource(R.string.ExtendedKey_Purpose),
                            items = viewModel.purposes.map {
                                TabItem(it.name, it == viewModel.purpose, it)
                            },
                            onDismissRequest = {
                                showPurposeSelectorDialog = false
                            },
                            onSelectItem = {
                                hidden = true
                                viewModel.set(it)
                            }
                        )
                    }
                    if (showBlockchainSelectorDialog) {
                        SelectorDialogCompose(
                            title = stringResource(R.string.ExtendedKey_Blockchain),
                            items = viewModel.blockchains.map {
                                TabItem(it.name, it == viewModel.blockchain, it)
                            },
                            onDismissRequest = {
                                showBlockchainSelectorDialog = false
                            },
                            onSelectItem = {
                                hidden = true
                                viewModel.set(it)
                            }
                        )
                    }
                    if (showAccountSelectorDialog) {
                        SelectorDialogCompose(
                            title = stringResource(R.string.ExtendedKey_Account),
                            items = viewModel.accounts.map {
                                TabItem(it.toString(), it == viewModel.account, it)
                            },
                            onDismissRequest = {
                                showAccountSelectorDialog = false
                            },
                            onSelectItem = {
                                hidden = true
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
                        TextHelper.copyText(viewModel.accountExtendedKey)
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
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
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
