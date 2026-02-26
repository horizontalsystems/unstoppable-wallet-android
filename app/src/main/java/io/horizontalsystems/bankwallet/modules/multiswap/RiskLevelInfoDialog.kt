package io.horizontalsystems.bankwallet.modules.multiswap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSBadgeOutline
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.core.findNavController
import kotlinx.serialization.Serializable

@Serializable
data object RiskLevelInfoScreen : HSScreen()

class RiskLevelInfoDialog : BaseComposableBottomSheetFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                val navController = findNavController()
                RiskLevelInfoScreen(navController)
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskLevelInfoScreen(navController: NavController) {
    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = navController::popBackStack,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            buttons = {
                HSButton(
                    title = stringResource(R.string.Button_Close),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    onClick = {
                        navController.popBackStack()
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
