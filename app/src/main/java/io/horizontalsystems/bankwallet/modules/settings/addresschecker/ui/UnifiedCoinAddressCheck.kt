package io.horizontalsystems.bankwallet.modules.settings.addresschecker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.adapters.StellarAssetAdapter
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.send.address.ui.CheckAddressInput
import io.horizontalsystems.bankwallet.modules.settings.addresschecker.CheckState
import io.horizontalsystems.bankwallet.modules.settings.addresschecker.IssueType
import io.horizontalsystems.bankwallet.modules.settings.addresschecker.UnifiedAddressCheckerViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedAddressCheckScreen(
    onClose: () -> Unit,
) {
    val viewModel =
        viewModel<UnifiedAddressCheckerViewModel>(factory = UnifiedAddressCheckerViewModel.Factory())

    val coroutineScope = rememberCoroutineScope()
    val infoModalBottomSheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var bottomCheckInfo by remember { mutableStateOf<BottomCheckInfo?>(null) }

    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Send_EnterAddress),
        onBack = onClose,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = onClose
            )
        )
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                CheckAddressInput(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    value = uiState.value,
                    hint = stringResource(id = R.string.Send_Hint_Address),
                    state = uiState.inputState,
                ) {
                    viewModel.onEnterAddress(it)
                }

                if (uiState.inputState is DataState.Error) {
                    ValidationError(uiState.inputState.error)
                } else {
                    SecurityCheckCard(
                        title = stringResource(R.string.SettingsAddressChecker_ChainalysisCheck),
                        icon = painterResource(R.drawable.ic_chainalysis),
                        description = stringResource(R.string.SettingsAddressChecker_ChainalysisCheckDescription),
                        onInfoClick = { info ->
                            bottomCheckInfo = info
                            coroutineScope.launch {
                                infoModalBottomSheetState.show()
                            }
                        }
                    ) {
                        NetworkItem(
                            title = stringResource(R.string.SettingsAddressChecker_SanctionsCheck),
                            status = uiState.checkResults[IssueType.Chainalysis]
                        )
                    }
                    VSpacer(16.dp)

                    SecurityCheckCard(
                        title = stringResource(R.string.SettingsAddressChecker_HashditCheck),
                        icon = painterResource(R.drawable.ic_hashdit),
                        description = stringResource(R.string.SettingsAddressChecker_HashditCheckDescription),
                        onInfoClick = { info ->
                            bottomCheckInfo = info
                            coroutineScope.launch {
                                infoModalBottomSheetState.show()
                            }
                        }
                    ) {
                        viewModel.hashDitBlockchains.forEach { blockchain ->
                            NetworkItem(
                                title = stringResource(
                                    R.string.SettingsAddressChecker_OnSubject,
                                    blockchain.name
                                ),
                                status = uiState.checkResults[IssueType.HashDit(blockchain.type)]
                            )
                        }
                    }
                    VSpacer(16.dp)

                    viewModel.contractFullCoins.forEach { fullCoin ->
                        val description = when (fullCoin.coin.uid) {
                            "tether" -> stringResource(R.string.SettingsAddressChecker_TetherBlacklistDescription)
                            "usd-coin" -> stringResource(R.string.SettingsAddressChecker_UsdcBlacklistDescription)
                            "paypal-usd" -> stringResource(R.string.SettingsAddressChecker_PyusdBlacklistDescription)
                            else -> stringResource(R.string.Error)
                        }
                        SecurityCheckCard(
                            title = stringResource(
                                R.string.SettingsAddressChecker_TokenBlacklistCheck,
                                fullCoin.coin.name
                            ),
                            icon = rememberAsyncImagePainter(
                                model = fullCoin.coin.imageUrl,
                                error = painterResource(R.drawable.coin_placeholder)
                            ),
                            description = description,
                            onInfoClick = { info ->
                                bottomCheckInfo = info
                                coroutineScope.launch {
                                    infoModalBottomSheetState.show()
                                }
                            }
                        ) {
                            fullCoin.tokens.forEach { token ->
                                NetworkItem(
                                    title = stringResource(
                                        R.string.SettingsAddressChecker_OnSubject,
                                        token.blockchain.name
                                    ),
                                    status = uiState.checkResults[IssueType.Contract(token)]
                                )
                            }
                        }
                        VSpacer(16.dp)
                    }

                    VSpacer(32.dp)
                }
            }
        }

        bottomCheckInfo?.let { info ->
            CheckInfoBottomSheet(
                title = info.title,
                description = info.description,
                bottomSheetState = infoModalBottomSheetState,
                hideBottomSheet = {
                    coroutineScope.launch {
                        infoModalBottomSheetState.hide()
                    }
                    bottomCheckInfo = null
                }
            )
        }
    }
}

@Composable
private fun ValidationError(addressValidationError: Throwable) {
    TextImportantError(
        modifier = Modifier.padding(horizontal = 16.dp),
        icon = R.drawable.ic_attention_20,
        title = stringResource(R.string.SwapSettings_Error_InvalidAddress),
        text = addressValidationError.getErrorMessage()
            ?: stringResource(R.string.SettingsAddressChecker_InvalidAddress)
    )
    VSpacer(32.dp)
}

@Composable
private fun Throwable.getErrorMessage() = when (this) {
    is StellarAssetAdapter.NoTrustlineError -> {
        stringResource(R.string.Error_AssetNotEnabled, code)
    }

    else -> this.message
}

@Composable
fun SecurityCheckCard(
    title: String,
    icon: Painter,
    description: String,
    onInfoClick: (BottomCheckInfo) -> Unit = {},
    results: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        backgroundColor = ComposeAppTheme.colors.tyler,
        border = BorderStroke(0.5.dp, ComposeAppTheme.colors.blade),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = "Check Icon",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )

                    HSpacer(12.dp)

                    subhead1_grey(title)
                }

                Icon(
                    painter = painterResource(R.drawable.ic_info_20),
                    contentDescription = "Info",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = {
                            onInfoClick.invoke(
                                BottomCheckInfo(title, description)
                            )
                        })
                )
            }

            HsDivider(modifier = Modifier.fillMaxWidth())

            results()
        }
    }
}

@Composable
fun NetworkItem(
    title: String,
    status: CheckState?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        subhead2_leah(title)

        status?.let {
            if (status == CheckState.Checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = ComposeAppTheme.colors.grey
                )
            } else {
                val color = when (status) {
                    CheckState.Clear -> ComposeAppTheme.colors.remus
                    CheckState.Detected -> ComposeAppTheme.colors.lucian
                    else -> ComposeAppTheme.colors.grey
                }
                Text(
                    text = stringResource(status.title),
                    color = color,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInfoBottomSheet(
    title: String,
    description: String,
    hideBottomSheet: () -> Unit,
    bottomSheetState: SheetState
) {

    ModalBottomSheet(
        onDismissRequest = hideBottomSheet,
        sheetState = bottomSheetState,
        containerColor = ComposeAppTheme.colors.transparent
    ) {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_info_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.grey),
            title = title,
            titleColor = ComposeAppTheme.colors.leah,
            onCloseClick = hideBottomSheet
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 24.dp)
                    .fillMaxWidth()
            ) {
                body_leah(
                    text = description,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                VSpacer(36.dp)
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Close),
                    onClick = hideBottomSheet
                )
                VSpacer(32.dp)
            }
        }
    }
}

@Preview
@Composable
fun SecurityCheckScreenPreview() {
    ComposeAppTheme {
        UnifiedAddressCheckScreen() {}
    }
}

data class BottomCheckInfo(
    val title: String,
    val description: String,
)