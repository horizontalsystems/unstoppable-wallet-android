package io.horizontalsystems.bankwallet.modules.usersubscription

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.PlanItems
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.TitleCenteredTopBar
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.highlightText
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.RadialBackground
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.subscriptions.core.IPaidAction
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class PremiumFeaturesFragment : BaseFragment() {

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
                ComposeAppTheme {
                    val navController = findNavController()
                    PremiumFeaturesScreen(
                        navController = navController,
                        navHostController = null,
                        onClose = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    @Parcelize
    data class Input(val action: IPaidAction) : Parcelable

    @Parcelize
    class Result : Parcelable
}

class PremiumFeaturesDialog : BaseComposableBottomSheetFragment() {

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
                ComposeAppTheme {
                    val navController = findNavController()
                    PremiumFeaturesScreen(
                        navController = navController,
                        navHostController = null,
                        onClose = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumFeaturesScreen(
    navController: NavController,
    navHostController: NavController?,
    onClose: () -> Unit
) {
    val viewModel = viewModel<BuySubscriptionViewModel> {
        BuySubscriptionViewModel()
    }

    val uiState = viewModel.uiState
    val hasFreeTrial = uiState.hasFreeTrial

    val coroutineScope = rememberCoroutineScope()
    val plansModalBottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isPlanSelectBottomSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            TitleCenteredTopBar(
                title = stringResource(R.string.Premium_Title),
                onCloseClick = onClose
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .navigationBarsPadding()
                .fillMaxSize()
        ) {
            RadialBackground()

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .padding(bottom = 70.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(24.dp)
                Image(
                    painter = painterResource(id = R.drawable.prem_star_launch),
                    contentDescription = null,
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                )
                VSpacer(24.dp)
                ActionText()
                VSpacer(24.dp)
                uiState.defenseSystemFeatures
                FeaturesSection(
                    navController = navController,
                    icon = R.drawable.defense_gradient_filled_24,
                    title = stringResource(R.string.Premium_DefenseSystem),
                    features = uiState.defenseSystemFeatures,
                )

                FeaturesSection(
                    navController = navController,
                    icon = R.drawable.market_gradient_filled_24,
                    title = stringResource(R.string.Premium_MarketInsights),
                    features = uiState.marketInsightsFeatures,
                )

                FeaturesSection(
                    navController = navController,
                    icon = R.drawable.heart_gradient_filled_24,
                    title = stringResource(R.string.Premium_Vip),
                    features = uiState.vipFeatures,
                )

                VSpacer(32.dp)
                headline2_leah(
                    text = stringResource(R.string.Premium_HighlyRatedSecurity),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
                VSpacer(24.dp)
                Image(
                    painter = painterResource(id = R.drawable.security_rate_image),
                    contentDescription = null,
                    modifier = Modifier
                        .height(112.dp)
                        .fillMaxWidth()
                )
                subhead2_grey(
                    text = stringResource(R.string.Premium_ApprovedBy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.bitcoin_logo),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.leah
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.wallet_scrutiny_logo),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.leah
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.certik_logo),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.leah
                    )
                }
                VSpacer(52.dp)
            }
            val buttonTitle = if (hasFreeTrial) {
                stringResource(R.string.Premium_TryForFree)
            } else {
                stringResource(R.string.Premium_Upgrade)
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    ComposeAppTheme.colors.transparent,
                                    ComposeAppTheme.colors.tyler
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .background(ComposeAppTheme.colors.tyler)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HSButton(
                        title = buttonTitle,
                        variant = ButtonVariant.Primary,
                        size = ButtonSize.Medium,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            //when used in NavHost
                            navHostController?.let {
                                coroutineScope.launch {
                                    isPlanSelectBottomSheetVisible = true
                                    plansModalBottomSheetState.show()
                                }
                            } ?: run {
                                onClose.invoke()
                                navController.slideFromBottom(R.id.selectSubscriptionPlanDialog)
                            }
                        }
                    )
                }
                VSpacer(16.dp)
            }
            if (isPlanSelectBottomSheetVisible) {
                SelectPlanBottomSheet(
                    onDismiss = {
                        coroutineScope.launch {
                            plansModalBottomSheetState.hide()
                            isPlanSelectBottomSheetVisible = false
                        }
                    },
                    onPurchase = {
                        coroutineScope.launch {
                            plansModalBottomSheetState.hide()
                            isPlanSelectBottomSheetVisible = false
                        }
                        navHostController?.navigate("premium_subscribed_page")
                    }
                )
            }
        }
    }
}

@Composable
fun FeaturesSection(
    navController: NavController,
    icon: Int,
    title: String,
    features: List<IPaidAction>
) {
    SectionHeader(title = title, icon)
    Column(
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
    ) {
        PlanItems(
            items = features,
            onItemClick = { action ->
                val feature = PremiumFeature.getFeature(action)
                navController.slideFromBottom(
                    R.id.defenseSystemFeatureDialog,
                    DefenseSystemFeatureDialog.Input(feature)
                )
            }
        )
    }
}

@Composable
private fun ActionText() {
    val text = highlightText(
        text = stringResource(R.string.Premium_Banner_BlackFridayText),
        textColor = ComposeAppTheme.colors.leah,
        highlightPart = stringResource(R.string.Premium_Title),
        highlightColor = ComposeAppTheme.colors.jacob
    )
    Text(
        text = text,
        style = ComposeAppTheme.typography.headline1,
        color = ComposeAppTheme.colors.leah,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    )
}

@Preview
@Composable
private fun PremiumFeaturesScreenPreview() {
    ComposeAppTheme {
        val ctx = LocalContext.current
        PremiumFeaturesScreen(
            navController = NavController(ctx),
            navHostController = null
        ) {}
    }
}