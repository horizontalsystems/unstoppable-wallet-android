package io.horizontalsystems.walletkit.modules.manageaccount.showextendedkey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
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
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.managers.FaqManager
import io.horizontalsystems.walletkit.core.stats.StatEntity
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.manageaccount.showextendedkey.ShowExtendedKeyModule.DisplayKeyType
import io.horizontalsystems.walletkit.modules.manageaccount.ui.ActionButton
import io.horizontalsystems.walletkit.modules.manageaccount.ui.ConfirmCopyBottomSheet
import io.horizontalsystems.walletkit.modules.manageaccount.ui.HidableContent
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HsIconButton
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.TextImportantWarning
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.body_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_grey
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.menu.MenuGroup
import io.horizontalsystems.walletkit.uiv3.components.menu.MenuItemX
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ShowExtendedKeyPage(val input: Input?) : HSPage(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val hdExtendedKey = input?.extendedRootKey
        val displayKeyType = input?.displayKeyType

        if (hdExtendedKey == null || displayKeyType == null) {
            NoExtendKeyScreen()
        } else {
            ShowExtendedKeyScreen(
                navigation,
                hdExtendedKey,
                displayKeyType
            )
        }
    }

    @Serializable
    data class Input(val extendedRootKeySerialized: String, val displayKeyType: DisplayKeyType) {
        val extendedRootKey: HDExtendedKey?
            get() = try {
                HDExtendedKey(extendedRootKeySerialized)
            } catch (error: Throwable) {
                null
            }

        constructor(extendedRootKey: HDExtendedKey, displayKeyType: DisplayKeyType) : this(
            extendedRootKey.serialize(),
            displayKeyType
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowExtendedKeyScreen(
    navigation: HSNavigation,
    extendedKey: HDExtendedKey,
    displayKeyType: DisplayKeyType
) {
    val viewModel = viewModel<ShowExtendedKeyViewModel>(
        factory = ShowExtendedKeyModule.Factory(
            extendedKey,
            displayKeyType
        )
    )

    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    HSScaffold(
        title = viewModel.title.getString(),
        onBack = navigation::removeLastOrNull,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Info_Title),
                icon = R.drawable.ic_info_24,
                onClick = {
                    FaqManager.showFaqPage(navigation, FaqManager.faqPathPrivateKeys)
                    viewModel.logEvent(StatEvent.Open(StatPage.Info))
                }
            )
        )
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                VSpacer(12.dp)

                if (viewModel.displayKeyType.isPrivate) {
                    TextImportantWarning(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.PrivateKeys_NeverShareWarning)
                    )
                    VSpacer(24.dp)
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
                                infoButtonClick = {
                                    navigation.slideFromBottom(KeyAccountInfoPage)
                                },
                                onClick = { showAccountSelectorDialog = true }
                            )
                        }
                    }
                }

                if (menuItems.isNotEmpty()) {
                    CellUniversalLawrenceSection(menuItems)
                }

                VSpacer(32.dp)
                if (viewModel.displayKeyType.isPrivate) {
                    HidableContent(
                        viewModel.extendedKey,
                        stringResource(R.string.ExtendedKey_TapToShowPrivateKey)
                    ) {
                        viewModel.logEvent(StatEvent.ToggleHidden)
                    }
                } else {
                    HidableContent(viewModel.extendedKey)
                }

                if (showPurposeSelectorDialog) {
                    MenuGroup(
                        title = stringResource(R.string.ExtendedKey_Purpose),
                        items = viewModel.purposes.map {
                            MenuItemX(it.name, it == viewModel.purpose, it)
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
                    MenuGroup(
                        title = stringResource(R.string.ExtendedKey_Blockchain),
                        items = viewModel.blockchains.map {
                            MenuItemX(it.name, it == viewModel.blockchain, it)
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
                    MenuGroup(
                        title = stringResource(R.string.ExtendedKey_Account),
                        items = viewModel.accounts.map {
                            MenuItemX(it.toString(), it == viewModel.account, it)
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
                    showBottomSheet = true
                } else {
                    TextHelper.copyText(viewModel.extendedKey)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)

                    viewModel.logEvent(StatEvent.Copy(StatEntity.Key))
                }
            }
        }
        if (showBottomSheet) {
            ConfirmCopyBottomSheet(
                sheetState = sheetState,
                onConfirm = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                    TextHelper.copyText(viewModel.extendedKey)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                    showBottomSheet = false

                    viewModel.logEvent(StatEvent.Copy(StatEntity.Key))
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }
            )
        }
    }
}

@Composable
fun MenuItem(
    title: String,
    value: String,
    infoButtonClick: (() -> Unit)? = null,
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
        infoButtonClick?.let { click ->
            HSpacer(width = 8.dp)
            HsIconButton(
                modifier = Modifier.size(20.dp),
                onClick = click
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info_20),
                    contentDescription = "info button",
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }

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
