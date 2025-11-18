package io.horizontalsystems.bankwallet.modules.premium

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
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
import io.horizontalsystems.subscriptions.core.AdvancedSearch
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.LossProtection
import io.horizontalsystems.subscriptions.core.PrioritySupport
import io.horizontalsystems.subscriptions.core.RobberyProtection
import io.horizontalsystems.subscriptions.core.ScamProtection
import io.horizontalsystems.subscriptions.core.SecureSend
import io.horizontalsystems.subscriptions.core.TokenInsights
import io.horizontalsystems.subscriptions.core.TradeSignals
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
                val input: Input = navController.getInput()
                    ?: run {
                        navController.popBackStack()
                        return@setContent
                    }

                ComposeAppTheme {
                    DefenseSystemFeatureScreen(
                        navController,
                        input.feature,
                        input.showAllFeaturesButton
                    )
                }
            }
        }
    }

    @Parcelize
    data class Input(val feature: PremiumFeature, val showAllFeaturesButton: Boolean = false) :
        Parcelable
}

@Parcelize
enum class PremiumFeature(
    val titleRes: Int,
    val descriptionRes: Int,
    val imageRes: Int,
) : Parcelable {
    SecureSendFeature(
        R.string.Premium_UpgradeFeature_SecureSend,
        R.string.Premium_UpgradeFeature_SecureSend_BigDescription,
        R.drawable.defense_secure_send
    ),
    LossProtectionFeature(
        R.string.Premium_UpgradeFeature_LossProtection,
        R.string.Premium_UpgradeFeature_LossProtection_BigDescription,
        R.drawable.defense_loss_protection
    ),
    ScamProtectionFeature(
        R.string.Premium_UpgradeFeature_ScamProtection,
        R.string.Premium_UpgradeFeature_ScamProtection_BigDescription,
        R.drawable.defense_scam_protection
    ),
    RobberyProtectionFeature(
        R.string.Premium_UpgradeFeature_RobberProtection,
        R.string.Premium_UpgradeFeature_RobberProtection_BigDescription,
        R.drawable.defense_robbery_protection
    ),
    TokenInsightsFeature(
        R.string.Premium_UpgradeFeature_TokenInsights,
        R.string.Premium_UpgradeFeature_TokenInsights_BigDescription,
        R.drawable.defense_token_insights
    ),
    AdvancedSearchFeature(
        R.string.Premium_UpgradeFeature_AdvancedSearch,
        R.string.Premium_UpgradeFeature_AdvancedSearch_BigDescription,
        R.drawable.defense_advanced_search
    ),
    TradeSignalsFeature(
        R.string.Premium_UpgradeFeature_TradeSignals,
        R.string.Premium_UpgradeFeature_TradeSignals_BigDescription,
        R.drawable.defense_trade_signals
    ),
    PrioritySupportFeature(
        R.string.Premium_UpgradeFeature_PrioritySupport,
        R.string.Premium_UpgradeFeature_PrioritySupport_BigDescription,
        R.drawable.defense_priority_support
    );

    companion object {
        fun getFeature(paidAction: IPaidAction) = when (paidAction) {
            TokenInsights -> TokenInsightsFeature
            AdvancedSearch -> AdvancedSearchFeature
            TradeSignals -> TradeSignalsFeature
            RobberyProtection -> RobberyProtectionFeature
            SecureSend -> SecureSendFeature
            ScamProtection -> ScamProtectionFeature
            PrioritySupport -> PrioritySupportFeature
            LossProtection -> LossProtectionFeature
            else -> throw IllegalArgumentException("Unknown paid action")
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefenseSystemFeatureScreen(
    navController: NavController,
    feature: PremiumFeature,
    showAllFeaturesButton: Boolean
) {
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
            if (showAllFeaturesButton) {
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
}
