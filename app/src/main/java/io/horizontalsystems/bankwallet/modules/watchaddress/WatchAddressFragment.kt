package io.horizontalsystems.bankwallet.modules.watchaddress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEffect
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import io.horizontalsystems.bankwallet.modules.restoreconfig.BirthdayHeightConfigScreen
import io.horizontalsystems.bankwallet.modules.watchaddress.selectblockchains.SelectBlockchainsScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputMultiline
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class WatchAddressScreen(
    val popOffOnSuccess: KClass<out HSScreen> = WatchAddressScreen::class,
    val popOffInclusive: Boolean = true
) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        WatchAddressScreen(backStack, popOffOnSuccess, popOffInclusive, resultBus)
    }
}

class WatchAddressFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
//        val input = navController.getInput<ManageAccountsModule.Input>()
//        val popUpToInclusiveId = input?.popOffOnSuccess ?: R.id.watchAddressFragment
//        val inclusive = input?.popOffInclusive ?: true
//        WatchAddressScreen(navController, popUpToInclusiveId, inclusive)
    }

}

@Composable
fun WatchAddressScreen(
    backStack: NavBackStack<HSScreen>,
    popUpToInclusiveId: KClass<out HSScreen>,
    inclusive: Boolean,
    resultBus: ResultEventBus
) {
    val view = LocalView.current

    val viewModel = viewModel<WatchAddressViewModel>(factory = WatchAddressModule.Factory())
    val uiState = viewModel.uiState
    val accountCreated = uiState.accountCreated
    val submitType = uiState.submitButtonType
    val accountType = uiState.accountType
    val accountName = uiState.accountName

    LaunchedEffect(accountCreated) {
        if (accountCreated) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_AddressAdded,
                icon = R.drawable.icon_binocule_24,
                iconTint = R.color.white
            )
            delay(300)
            backStack.removeLastUntil(popUpToInclusiveId, inclusive)
        }
    }

    if (accountType != null) {
        viewModel.blockchainSelectionOpened()

        backStack.add(SelectBlockchainsScreen(
            popUpToInclusiveId,
            inclusive,
            accountType,
            accountName
        ))
    }

    ResultEffect<BirthdayHeightConfigScreen.Result>(resultBus) { result ->
        if (result.config != null) {
            viewModel.onBirthdayHeightEntered(result.config.birthdayHeight?.toLongOrNull())
        } else {
            viewModel.onBirthdayHeightCancelled()
        }
    }

    if (uiState.openBirthdayHeightScreen) {
        viewModel.onBirthdayHeightScreenOpened()
        backStack.add(BirthdayHeightConfigScreen(BlockchainType.Monero))
    }

    HSScaffold(
        title = stringResource(R.string.ManageAccounts_WatchAddress),
        onBack = backStack::removeLastOrNull,
        menuItems = buildList {
            when (submitType) {
                is SubmitButtonType.Watch -> {
                    add(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Done),
                            onClick = viewModel::onClickWatch,
                            enabled = submitType.enabled,
                            tint = ComposeAppTheme.colors.jacob
                        )
                    )
                }

                is SubmitButtonType.Next -> {
                    add(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Next),
                            onClick = viewModel::onClickNext,
                            enabled = submitType.enabled
                        )
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(12.dp)

            HeaderText(stringResource(id = R.string.ManageAccount_Name))
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = viewModel.accountName,
                pasteEnabled = false,
                hint = viewModel.defaultAccountName,
                onValueChange = viewModel::onEnterAccountName
            )
            VSpacer(32.dp)
            FormsInputMultiline(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = uiState.inputState?.dataOrNull,
                hint = stringResource(id = R.string.Watch_Address_Hint),
                qrScannerEnabled = true,
                state = uiState.inputState,
                onValueChange = {
                    viewModel.onEnterInput(it)
                },
                onClear = {
                    stat(page = StatPage.WatchWallet, event = StatEvent.Clear(StatEntity.Key))
                },
                onScanQR = {
                    stat(page = StatPage.WatchWallet, event = StatEvent.ScanQr(StatEntity.Key))
                },
                onPaste = {
                    stat(page = StatPage.WatchWallet, event = StatEvent.Paste(StatEntity.Key))
                }
            )

            if (uiState.addressType == WatchAddressViewModel.Type.MoneroAddress) {
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    initial = uiState.viewKeyState?.dataOrNull,
                    pasteEnabled = true,
                    hint = stringResource(R.string.Watch_ViewKey),
                    onValueChange = viewModel::onEnterViewKey,
                    state = uiState.viewKeyState
                )
            }

            VSpacer(32.dp)
        }
    }
}
