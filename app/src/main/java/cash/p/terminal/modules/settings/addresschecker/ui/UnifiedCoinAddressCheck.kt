package cash.p.terminal.modules.settings.addresschecker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.adapters.stellar.StellarAssetAdapter
import cash.p.terminal.modules.send.address.ui.CheckAddressInput
import cash.p.terminal.modules.settings.addresschecker.CheckState
import cash.p.terminal.modules.settings.addresschecker.IssueType
import cash.p.terminal.modules.settings.addresschecker.UnifiedAddressCheckerViewModel
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsDivider
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.TextImportantError
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.entities.DataState
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.ui_compose.theme.YellowL
import cash.p.terminal.wallet.imageUrl
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedAddressCheckScreen(
    initialAddress: String? = null,
    onClose: () -> Unit,
    onPremiumClick: () -> Unit,
) {
    val viewModel =
        viewModel<UnifiedAddressCheckerViewModel>(factory = UnifiedAddressCheckerViewModel.Factory(initialAddress))

    val coroutineScope = rememberCoroutineScope()
    val infoModalBottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var bottomCheckInfo by remember { mutableStateOf<BottomCheckInfo?>(null) }

    val uiState = viewModel.uiState

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Send_EnterAddress),
                navigationIcon = {
                    HsBackButton(onClick = onClose)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close_24,
                        onClick = onClose
                    )
                )
            )
        },
    ) { innerPaddings ->
        Column(
            modifier = Modifier
                .padding(innerPaddings)
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
                        title = stringResource(R.string.smart_contract_check),
                        icon = painterResource(R.drawable.ic_star_filled_20),
                        description = stringResource(R.string.smart_contract_check_desription),
                        onInfoClick = { info ->
                            bottomCheckInfo = info
                            coroutineScope.launch {
                                infoModalBottomSheetState.show()
                            }
                        }
                    ) {
                        NetworkItem(
                            title = stringResource(R.string.smart_contract_check),
                            status = uiState.checkResults[IssueType.SmartContract],
                            onClick = if (uiState.checkResults[IssueType.SmartContract] == CheckState.Locked) {
                                { onPremiumClick() }
                            } else {
                                null
                            }
                        )
                    }
                    VSpacer(16.dp)

                    SecurityCheckCard(
                        title = stringResource(R.string.alpha_aml_title),
                        icon = painterResource(R.drawable.ic_alpha_aml),
                        infoTitle = stringResource(R.string.alpha_aml_check_title_description),
                        description = stringResource(R.string.alpha_aml_description),
                        onInfoClick = { info ->
                            bottomCheckInfo = info
                            coroutineScope.launch {
                                infoModalBottomSheetState.show()
                            }
                        }
                    ) {
                        NetworkItem(
                            title = stringResource(R.string.alpha_aml_check_title_description),
                            status = uiState.checkResults[IssueType.AlphaAml]
                        )
                    }
                    VSpacer(16.dp)

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
    infoTitle: String = title,
    description: String,
    onInfoClick: (BottomCheckInfo) -> Unit = {},
    results: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        backgroundColor = ComposeAppTheme.colors.tyler,
        border = BorderStroke(0.5.dp, ComposeAppTheme.colors.andy),
        shape = RoundedCornerShape(12.dp),
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

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            color = ComposeAppTheme.colors.leah,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Icon(
                    painter = painterResource(R.drawable.ic_info_20),
                    contentDescription = "Info",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = {
                            onInfoClick.invoke(
                                BottomCheckInfo(infoTitle, description)
                            )
                        })
                )
            }

            HsDivider(
                color = ComposeAppTheme.colors.andy,
                modifier = Modifier.fillMaxWidth()
            )

            results()
        }
    }
}

@Composable
fun NetworkItem(
    title: String,
    status: CheckState?,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { base ->
                if (onClick != null) base.clickable(onClick = onClick) else base
            }
            .height(40.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ComposeAppTheme.colors.bran,
            fontSize = 14.sp
        )

        status?.let {
            when (status) {
                CheckState.Checking -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ComposeAppTheme.colors.grey
                    )
                }

                CheckState.Locked -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.andy,
                        modifier = Modifier.size(16.dp),
                    )
                }

                else -> {
                    val color = when (status) {
                        CheckState.Clear -> ComposeAppTheme.colors.remus
                        CheckState.Detected -> ComposeAppTheme.colors.lucian
                        CheckState.AlphaAmlVeryLow -> ComposeAppTheme.colors.remus
                        CheckState.AlphaAmlLow -> ComposeAppTheme.colors.yellowD
                        CheckState.AlphaAmlHigh -> YellowL
                        CheckState.AlphaAmlVeryHigh -> ComposeAppTheme.colors.lucian
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
        UnifiedAddressCheckScreen(
            onClose = {},
            onPremiumClick = {}
        )
    }
}

data class BottomCheckInfo(
    val title: String,
    val description: String,
)