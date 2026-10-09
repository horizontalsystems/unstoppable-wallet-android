package io.horizontalsystems.walletkit.modules.nearaccount

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.shorten
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.modules.enablecoin.restoresettings.AccountPickerResult
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import io.horizontalsystems.walletkit.modules.nav3.observeResult
import io.horizontalsystems.walletkit.ui.compose.components.HSCircularProgressIndicator
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.subhead_grey
import io.horizontalsystems.walletkit.uiv3.components.AlertCard
import io.horizontalsystems.walletkit.uiv3.components.AlertFormat
import io.horizontalsystems.walletkit.uiv3.components.AlertType
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.cell.CellGroup
import io.horizontalsystems.walletkit.uiv3.components.cell.CellLeftSelectors
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import io.horizontalsystems.walletkit.uiv3.components.info.TextBlock
import kotlinx.serialization.Serializable

/** Picks the NEAR account a recovery phrase opens: its implicit account or a named one its key controls. */
@Serializable
data class NearAccountPickerPage(val accountType: AccountType) : HSPage(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val mnemonic = accountType as? AccountType.Mnemonic ?: return
        val viewModel = viewModel<NearAccountPickerViewModel>(factory = NearAccountPickerViewModel.Factory(mnemonic))
        val resultEventBus = LocalResultEventBus.current
        val uiState = viewModel.uiState

        LaunchedEffect(uiState.result) {
            uiState.result?.let {
                resultEventBus.sendResult(it)
                navigation.removeLastOrNull()
            }
        }

        BackHandler { viewModel.onCancel() }

        val nameKey = observeResult<NearAccountNamePage.Result> {
            viewModel.onAccountEntered(it.accountId)
        }
        val openNameEntry = {
            uiState.publicKey?.let { navigation.slideFromRightForResult(NearAccountNamePage(it), nameKey) }
        }

        NearAccountPickerScreen(
            uiState = uiState,
            onBack = viewModel::onCancel,
            onSelect = viewModel::onSelect,
            onEnterName = { openNameEntry() },
            onRetry = viewModel::retry,
            onUseDefault = viewModel::onUseDefault,
            onContinue = viewModel::onContinue,
        )
    }
}

@Composable
private fun NearAccountPickerScreen(
    uiState: NearAccountPickerUiState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onEnterName: () -> Unit,
    onRetry: () -> Unit,
    onUseDefault: () -> Unit,
    onContinue: () -> Unit,
) {
    HSScaffold(
        title = stringResource(R.string.NearAccount_Title),
        onBack = onBack,
    ) {
        when {
            uiState.searching || uiState.result != null -> Searching()
            uiState.searchFailed -> SearchFailed(
                onRetry = onRetry,
                onEnterName = onEnterName,
                onUseDefault = onUseDefault,
            )
            else -> AccountList(
                uiState = uiState,
                onSelect = onSelect,
                onEnterName = onEnterName,
                onContinue = onContinue,
            )
        }
    }
}

@Composable
private fun Searching() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HSCircularProgressIndicator()
        VSpacer(16.dp)
        subhead_grey(
            text = stringResource(R.string.NearAccount_Searching),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SearchFailed(
    onRetry: () -> Unit,
    onEnterName: () -> Unit,
    onUseDefault: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        VSpacer(12.dp)
        AlertCard(
            format = AlertFormat.Structured,
            type = AlertType.Caution,
            text = stringResource(R.string.NearAccount_SearchFailed),
        )
        VSpacer(24.dp)
        HSButton(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.Button_Retry),
            onClick = onRetry,
        )
        VSpacer(12.dp)
        HSButton(
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Secondary,
            title = stringResource(R.string.NearAccount_EnterName),
            onClick = onEnterName,
        )
        VSpacer(12.dp)
        HSButton(
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Secondary,
            style = ButtonStyle.Transparent,
            title = stringResource(R.string.NearAccount_UseDefault),
            onClick = onUseDefault,
        )
        VSpacer(32.dp)
    }
}

@Composable
private fun AccountList(
    uiState: NearAccountPickerUiState,
    onSelect: (String) -> Unit,
    onEnterName: () -> Unit,
    onContinue: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TextBlock(stringResource(R.string.NearAccount_Description))
            CellGroup(paddingValues = PaddingValues(horizontal = 16.dp)) {
                uiState.items.forEach { item ->
                    CellPrimary(
                        left = { CellLeftSelectors(selected = item.selected) },
                        middle = {
                            CellMiddleInfo(
                                title = (if (item.implicit) item.accountId.shorten() else item.accountId).hs,
                                subtitle = listOfNotNull(
                                    item.balance,
                                    if (item.implicit) stringResource(R.string.NearAccount_DefaultAccount) else null,
                                ).joinToString(" · ").takeIf { it.isNotEmpty() }?.hs,
                            )
                        },
                        onClick = { onSelect(item.accountId) },
                    )
                }
            }
            VSpacer(16.dp)
            CellGroup(paddingValues = PaddingValues(horizontal = 16.dp)) {
                CellPrimary(
                    middle = { CellMiddleInfo(title = stringResource(R.string.NearAccount_EnterName).hs) },
                    right = { CellRightNavigation() },
                    onClick = onEnterName,
                )
            }
            VSpacer(120.dp)
        }
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            HSButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.Button_Continue),
                enabled = uiState.continueEnabled,
                onClick = onContinue,
            )
            VSpacer(16.dp)
        }
    }
}
