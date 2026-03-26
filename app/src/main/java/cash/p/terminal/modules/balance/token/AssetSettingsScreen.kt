package cash.p.terminal.modules.balance.token

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.modules.displayoptions.DisplayDiffOptionType
import cash.p.terminal.modules.displayoptions.DisplayPricePeriod
import cash.p.terminal.modules.displayoptions.PriceParametersSection
import cash.p.terminal.modules.transactions.AmlCheckInfoBottomSheet
import cash.p.terminal.modules.transactions.AmlCheckRow
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui.compose.components.AlertGroup
import cash.p.terminal.ui_compose.Select
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsSettingCell
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun AssetSettingsScreen(
    amlCheckEnabled: Boolean,
    onAmlCheckChange: (Boolean) -> Unit,
    pricePeriod: DisplayPricePeriod,
    displayDiffOptionType: DisplayDiffOptionType,
    isRoundingAmount: Boolean,
    onPricePeriodChange: (DisplayPricePeriod) -> Unit,
    onDisplayDiffOptionTypeChange: (DisplayDiffOptionType) -> Unit,
    onRoundingAmountChange: (Boolean) -> Unit,
    onAddressPoisoningViewClick: () -> Unit,
    navController: NavController,
    onBack: () -> Unit,
) {
    var showAmlInfoSheet by remember { mutableStateOf(false) }
    var showPeriodSelector by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Settings_Title),
                navigationIcon = { HsBackButton(onClick = onBack) }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(12.dp)
            AmlCheckRow(
                enabled = amlCheckEnabled,
                onToggleChange = onAmlCheckChange,
                onInfoClick = { showAmlInfoSheet = true }
            )
            VSpacer(16.dp)

            PriceParametersSection(
                pricePeriod = pricePeriod,
                displayDiffOptionType = displayDiffOptionType,
                isRoundingAmount = isRoundingAmount,
                onPricePeriodClick = { showPeriodSelector = true },
                onPercentChangeToggled = { enabled ->
                    onDisplayDiffOptionTypeChange(
                        DisplayDiffOptionType.fromFlags(
                            priceChange = displayDiffOptionType.hasPriceChange,
                            percentChange = enabled
                        )
                    )
                },
                onPriceChangeToggled = { enabled ->
                    onDisplayDiffOptionTypeChange(
                        DisplayDiffOptionType.fromFlags(
                            priceChange = enabled,
                            percentChange = displayDiffOptionType.hasPercentChange
                        )
                    )
                },
                onRoundingAmountToggled = onRoundingAmountChange,
            )
            VSpacer(32.dp)
            CellUniversalLawrenceSection(
                listOf {
                    HsSettingCell(
                        title = R.string.address_poisoning_view,
                        icon = R.drawable.ic_flask_20,
                        onClick = onAddressPoisoningViewClick
                    )
                }
            )
        }
    }

    if (showAmlInfoSheet) {
        AmlCheckInfoBottomSheet(
            onPremiumSettingsClick = {
                showAmlInfoSheet = false
                navController.slideFromRight(R.id.premiumSettingsFragment)
            },
            onLaterClick = { showAmlInfoSheet = false },
            onDismiss = { showAmlInfoSheet = false }
        )
    }

    if (showPeriodSelector) {
        AlertGroup(
            title = R.string.display_options_price_period,
            select = Select(pricePeriod, DisplayPricePeriod.entries),
            onSelect = { selected ->
                onPricePeriodChange(selected)
                showPeriodSelector = false
            },
            onDismiss = { showPeriodSelector = false }
        )
    }
}

@Preview
@Composable
private fun AssetSettingsScreenPreview() {
    ComposeAppTheme {
        AssetSettingsScreen(
            amlCheckEnabled = true,
            onAmlCheckChange = {},
            pricePeriod = DisplayPricePeriod.ONE_DAY,
            displayDiffOptionType = DisplayDiffOptionType.BOTH,
            isRoundingAmount = false,
            onPricePeriodChange = {},
            onDisplayDiffOptionTypeChange = {},
            onRoundingAmountChange = {},
            onAddressPoisoningViewClick = {},
            navController = rememberNavController(),
            onBack = {},
        )
    }
}
