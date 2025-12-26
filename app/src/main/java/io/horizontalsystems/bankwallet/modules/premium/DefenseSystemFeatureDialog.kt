package io.horizontalsystems.bankwallet.modules.premium

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.settings.banners.TextWithDynamicScale
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.DynamicSliderIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.ButtonsStack
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton
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
        R.drawable.prem_securesend
    ),
    LossProtectionFeature(
        R.string.Premium_UpgradeFeature_LossProtection,
        R.string.Premium_UpgradeFeature_LossProtection_BigDescription,
        R.drawable.prem_lossprotection
    ),
    ScamProtectionFeature(
        R.string.Premium_UpgradeFeature_ScamProtection,
        R.string.Premium_UpgradeFeature_ScamProtection_BigDescription,
        R.drawable.prem_scamprotection
    ),
    RobberyProtectionFeature(
        R.string.Premium_UpgradeFeature_RobberyProtection,
        R.string.Premium_UpgradeFeature_RobberyProtection_BigDescription,
        R.drawable.prem_robberyprotection
    ),
    TokenInsightsFeature(
        R.string.Premium_UpgradeFeature_TokenInsights,
        R.string.Premium_UpgradeFeature_TokenInsights_BigDescription,
        R.drawable.prem_tokeninsight
    ),
    AdvancedSearchFeature(
        R.string.Premium_UpgradeFeature_AdvancedSearch,
        R.string.Premium_UpgradeFeature_AdvancedSearch_BigDescription,
        R.drawable.prem_advancedsearch
    ),
    TradeSignalsFeature(
        R.string.Premium_UpgradeFeature_TradeSignals,
        R.string.Premium_UpgradeFeature_TradeSignals_BigDescription,
        R.drawable.prem_tradesignals
    ),
    PrioritySupportFeature(
        R.string.Premium_UpgradeFeature_PrioritySupport,
        R.string.Premium_UpgradeFeature_PrioritySupport_BigDescription,
        R.drawable.prem_prioritysupport
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DefenseSystemFeatureScreen(
    navController: NavController,
    feature: PremiumFeature,
    showAllFeaturesButton: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val features = remember { PremiumFeature.entries.toTypedArray() }
    val pagerState = rememberPagerState(initialPage = features.indexOf(feature)) { features.size }

    BottomSheetContent(
        onDismissRequest = {
            navController.popBackStack()
        },
        sheetState = sheetState
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            Image(
                                modifier = Modifier.fillMaxWidth(),
                                painter = painterResource(R.drawable.prem_background),
                                contentScale = ContentScale.FillWidth,
                                contentDescription = null,
                            )
                            Image(
                                painter = painterResource(id = features[page].imageRes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth,
                            )
                        }

                        val currentFeature = features[page]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            body_leah(
                                text = stringResource(currentFeature.titleRes),
                                modifier = Modifier.padding(top = 16.dp),
                                textAlign = TextAlign.Center
                            )
                            val lineHeight = 24.sp
                            val threeLinesHeight = with(LocalDensity.current) {
                                (lineHeight * 3).toDp()
                            }
                            VSpacer(12.dp)
                            Box(
                                modifier = Modifier.height(threeLinesHeight)
                            ) {
                                TextWithDynamicScale(
                                    maxLines = 3,
                                    text = stringResource(currentFeature.descriptionRes),
                                    style = ComposeAppTheme.typography.body,
                                    color = ComposeAppTheme.colors.leah,
                                    textAlignment = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    HSIconButton(
                        icon = painterResource(id = R.drawable.ic_close),
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Small,
                        onClick = { navController.popBackStack() }
                    )
                }
            }

            VSpacer(16.dp)

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                DynamicSliderIndicator(
                    total = features.size,
                    current = pagerState.currentPage
                )
            }

            if (showAllFeaturesButton) {
                subhead1_jacob(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.popBackStack()
                            navController.slideFromBottom(R.id.buySubscriptionDialog)
                        }
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                    text = stringResource(R.string.Premium_OnePurchaseUnlocksAllPremium),
                    textAlign = TextAlign.Center
                )
            }
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
        }
    }
}
