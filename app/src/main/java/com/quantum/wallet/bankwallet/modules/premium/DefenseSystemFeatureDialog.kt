package com.quantum.wallet.bankwallet.modules.premium

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
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.getInput
import com.quantum.wallet.bankwallet.core.slideFromBottom
import com.quantum.wallet.bankwallet.modules.settings.banners.TextWithDynamicScale
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.DynamicSliderIndicator
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.body_leah
import com.quantum.wallet.bankwallet.ui.compose.components.subhead1_jacob
import com.quantum.wallet.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import com.quantum.wallet.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import com.quantum.wallet.bankwallet.uiv3.components.bottomsheet.ButtonsStack
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonSize
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonVariant
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSButton
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSIconButton
import com.quantum.wallet.core.findNavController
import com.quantum.wallet.subscriptions.core.AdvancedSearch
import com.quantum.wallet.subscriptions.core.IPaidAction
import com.quantum.wallet.subscriptions.core.PrioritySupport
import com.quantum.wallet.subscriptions.core.RobberyProtection
import com.quantum.wallet.subscriptions.core.ScamProtection
import com.quantum.wallet.subscriptions.core.SecureSend
import com.quantum.wallet.subscriptions.core.SwapProtection
import com.quantum.wallet.subscriptions.core.TokenInsights
import com.quantum.wallet.subscriptions.core.TradeSignals
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
                    )
                }
            }
        }
    }

    @Parcelize
    data class Input(val feature: PremiumFeature) :
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
    ScamProtectionFeature(
        R.string.Premium_UpgradeFeature_ScamProtection,
        R.string.Premium_UpgradeFeature_ScamProtection_BigDescription,
        R.drawable.prem_scamprotection
    ),
    SwapProtectionFeature(
        R.string.Premium_UpgradeFeature_SwapProtection,
        R.string.Premium_UpgradeFeature_SwapProtection_BigDescription,
        R.drawable.prem_swapprotection
    ),
    RobberyProtectionFeature(
        R.string.Premium_UpgradeFeature_RobberyProtection,
        R.string.Premium_UpgradeFeature_RobberyProtection_BigDescription,
        R.drawable.prem_robberyprotection
    ),
    PrioritySupportFeature(
        R.string.Premium_UpgradeFeature_PrioritySupport,
        R.string.Premium_UpgradeFeature_PrioritySupport_BigDescription,
        R.drawable.prem_prioritysupport
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
            SwapProtection -> SwapProtectionFeature
            else -> throw IllegalArgumentException("Unknown paid action")
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DefenseSystemFeatureScreen(
    navController: NavController,
    feature: PremiumFeature,
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
