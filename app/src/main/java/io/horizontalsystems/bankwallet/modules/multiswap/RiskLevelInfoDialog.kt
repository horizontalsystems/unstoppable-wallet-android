package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.multiswap.providers.RiskLevel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.HSString
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSBadgeOutline
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock

class RiskLevelInfoDialog : BaseComposableBottomSheetFragment() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        RiskLevelInfoScreen(navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskLevelInfoScreen(navController: NavBackStack<HSScreen>) {
    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = navController::removeLastOrNull,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            buttons = {
                HSButton(
                    title = stringResource(R.string.Button_Close),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    onClick = {
                        navController.removeLastOrNull()
                    }
                )
            },
            content = {
                BottomSheetHeaderV3(
                    title = stringResource(R.string.RiskLevel_ProviderRiskLevel)
                )
                TextBlock(
                    text = stringResource(R.string.RiskLevel_ProviderRiskLevel_Description),
                )
                VSpacer(20.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
                        .background(ComposeAppTheme.colors.lawrence),
                ) {
                    RiskLevelList()
                }
            }
        )
    }
}

@Composable
fun RiskLevelList() {
    CellPrimary(
        middle = {
            RiskLevelCell(
                RiskLevel.AUTO,
                stringResource(R.string.RiskLevel_Auto_Description)
            )

        }
    )
    HsDivider()
    CellPrimary(
        middle = {
            RiskLevelCell(
                RiskLevel.PRECHECK,
                stringResource(R.string.RiskLevel_Precheck_Description)
            )
        }
    )
    HsDivider()
    CellPrimary(
        middle = {
            RiskLevelCell(
                RiskLevel.LIMITED,
                stringResource(R.string.RiskLevel_Limited_Description)
            )
        }
    )
    HsDivider()
    CellPrimary(
        middle = {
            RiskLevelCell(
                RiskLevel.CONTROLLED,
                stringResource(R.string.RiskLevel_Controlled_Description)
            )
        }
    )
}

@Composable
private fun RiskLevelCell(level: RiskLevel, description: String) {
    val riskLevel = getRiskLevelHsString(level)
    Column {
        HSBadgeOutline(
            text = riskLevel.text,
            color = riskLevel.color ?: ComposeAppTheme.colors.grey
        )
        VSpacer(4.dp)
        CellMiddleInfo(
            subtitle = description.hs
        )
    }
}

@Composable
private fun getRiskLevelHsString(riskLevel: RiskLevel): HSString {
    val text = stringResource(riskLevel.title)
    val color = when (riskLevel) {
        RiskLevel.AUTO -> ComposeAppTheme.colors.remus
        RiskLevel.LIMITED -> ComposeAppTheme.colors.ocean
        RiskLevel.CONTROLLED -> ComposeAppTheme.colors.jacob
        RiskLevel.PRECHECK -> ComposeAppTheme.colors.leah
    }
    return HSString(text, color, false)
}
