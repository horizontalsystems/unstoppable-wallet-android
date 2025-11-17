package io.horizontalsystems.bankwallet.modules.premium

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.ButtonsStack
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.core.findNavController
import kotlinx.parcelize.Parcelize

class DefenseSystemFeatureDialog : BaseComposableBottomSheetFragment() {
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
                val feature: PremiumFeature = navController.getInput()
                    ?: run {
                        navController.popBackStack()
                        return@setContent
                    }

                ComposeAppTheme {
                    DefenseSystemFeatureScreen(navController, feature)
                }
            }
        }
    }
}

@Parcelize
enum class PremiumFeature(
    val titleRes: Int,
    val descriptionRes: Int,
    val imageRes: Int,
) : Parcelable {
    SecureSend(
        R.string.Premium_SecureSend,
        R.string.Premium_SecureSend_Description,
        R.drawable.defense_secure_send
    ),
    LossProtection(
        R.string.Premium_LossProtection,
        R.string.Premium_LossProtection_Description,
        R.drawable.defense_loss_protection
    ),
    ScamProtection(
        R.string.Premium_ScamProtection,
        R.string.Premium_ScamProtection_Description,
        R.drawable.defense_scam_protection
    ),
    RobberyProtection(
        R.string.Premium_RobberyProtection,
        R.string.Premium_RobberyProtection_Description,
        R.drawable.defense_robbery_protection
    ),
    TokenInsights(
        R.string.Premium_TokenInsights,
        R.string.Premium_TokenInsights_Description,
        R.drawable.defense_token_insights
    ),
    AdvancedSearch(
        R.string.Premium_AdvancedSearch,
        R.string.Premium_AdvancedSearch_Description,
        R.drawable.defense_advanced_search
    ),
    TradeSignals(
        R.string.Premium_TradeSignals,
        R.string.Premium_TradeSignals_Description,
        R.drawable.defense_trade_signals
    ),
    PrioritySupport(
        R.string.Premium_PrioritySupport,
        R.string.Premium_PrioritySupport_Description,
        R.drawable.defense_priority_support
    );
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefenseSystemFeatureScreen(navController: NavController, feature: PremiumFeature) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BottomSheetContent(
        onDismissRequest = {
            navController.popBackStack()
        },
        sheetState = sheetState
    ) {
        Column {
            BottomSheetHeaderV3(
                image400 = painterResource(feature.imageRes),
                title = stringResource(feature.titleRes),
                onCloseClick = {
                    navController.popBackStack()
                }
            )
            subhead_grey(
                text = stringResource(R.string.Premium_DefenseSystem),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
            TextBlock(
                text = stringResource(feature.descriptionRes),
                textAlign = TextAlign.Center
            )
        }
        ButtonsStack {
            HSButton(
                title = stringResource(R.string.Premium_TryForFree),
                variant = ButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    navController.popBackStack()
                    navController.slideFromBottom(R.id.selectSubscriptionPlanDialog)
                }
            )
            HSButton(
                title = stringResource(R.string.Premium_ViewAllFeatures),
                style = ButtonStyle.Transparent,
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Medium,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    navController.popBackStack()
                    navController.slideFromBottom(R.id.buySubscriptionDialog)
                }
            )
        }
    }
}
