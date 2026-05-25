package io.horizontalsystems.bankwallet.modules.watchaddress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.restoreconfig.BirthdayHeightConfig
import io.horizontalsystems.bankwallet.modules.watchaddress.selectblockchains.SelectBlockchainsPage
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputMultiline
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class WatchAddressPage(val input: ManageAccountsModule.Input? = null) : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        val popUpToInclusiveId = input?.popOffOnSuccess ?: WatchAddressPage::class
        val inclusive = input?.popOffInclusive ?: true
        WatchAddressScreen(navController, popUpToInclusiveId, inclusive)
    }

}

@Composable
fun WatchAddressScreen(navController: HSNavigation, popUpToInclusiveId: KClass<out HSPage>, inclusive: Boolean) {
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
            navController.removeLastUntil(popUpToInclusiveId, inclusive)
        }
    }

    if (accountType != null) {
        viewModel.blockchainSelectionOpened()

        navController.slideFromRight(
            SelectBlockchainsPage(SelectBlockchainsPage.Input(
                popUpToInclusiveId,
                inclusive,
                accountType,
                accountName
            ))
        )
    }

    if (uiState.openBirthdayHeightScreen) {
        viewModel.onBirthdayHeightScreenOpened()

        val forResult = navController.slideFromRightForResult<BirthdayHeightConfig.Result>(
            { BirthdayHeightConfig(BlockchainType.Monero) }
        ) { result ->
            if (result.config != null) {
                viewModel.onBirthdayHeightEntered(result.config.birthdayHeight?.toLongOrNull())
            } else {
                viewModel.onBirthdayHeightCancelled()
            }
        }
        forResult()
    }

    HSScaffold(
        title = stringResource(R.string.ManageAccounts_WatchAddress),
        onBack = navController::removeLastOrNull,
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
                onValueChange = viewModel::onEnterAccountName,
                trailingContent = {
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        HSIconButton(
                            variant = ButtonVariant.Secondary,
                            size = ButtonSize.Small,
                            icon = painterResource(R.drawable.ic_swap_circle_24),
                            onClick = viewModel::generateRandomAccountName
                        )
                    }
                }
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

            VSpacer(72.dp)
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            ButtonsGroupWithShade {
                when (submitType) {
                    is SubmitButtonType.Watch -> HSButton(
                        title = stringResource(R.string.Button_Done),
                        variant = ButtonVariant.Primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        enabled = submitType.enabled,
                        onClick = viewModel::onClickWatch,
                    )
                    is SubmitButtonType.Next -> HSButton(
                        title = stringResource(R.string.Button_Next),
                        variant = ButtonVariant.Primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        enabled = submitType.enabled,
                        onClick = viewModel::onClickNext,
                    )
                }
            }
        }
    }
}
