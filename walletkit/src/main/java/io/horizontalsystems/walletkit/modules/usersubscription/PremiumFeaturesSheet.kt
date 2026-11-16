package io.horizontalsystems.walletkit.modules.usersubscription

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.usersubscription.BuySubscriptionModel.descriptionStringRes
import io.horizontalsystems.walletkit.modules.usersubscription.BuySubscriptionModel.iconRes
import io.horizontalsystems.walletkit.modules.usersubscription.BuySubscriptionModel.titleStringRes
import io.horizontalsystems.walletkit.modules.usersubscription.ui.PremiumPage
import io.horizontalsystems.walletkit.modules.usersubscription.ui.highlightText
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.TestTags
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

@Composable
fun PremiumFeaturesScreen(
    navigation: HSNavigation,
    onClose: () -> Unit
) {
    val viewModel = viewModel<BuySubscriptionViewModel> {
        BuySubscriptionViewModel()
    }

    val uiState = viewModel.uiState
    var isPlanSelectBottomSheetVisible by remember { mutableStateOf(false) }

    PremiumPage(
        onClose = onClose,
        closeTestTag = TestTags.PREMIUM_CLOSE,
        button = {
            HSButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.PREMIUM_SUBSCRIBE),
                variant = ButtonVariant.Secondary,
                title = if (uiState.hasFreeTrial) {
                    stringResource(R.string.Premium_TryForFree)
                } else {
                    stringResource(R.string.Premium_Upgrade)
                },
                onClick = { isPlanSelectBottomSheetVisible = true }
            )
        }
    ) {
        TitleText()
        VSpacer(32.dp)
        FeaturesSection(
            features = uiState.advancedControlsFeatures + uiState.marketInsightsFeatures,
        )
        VSpacer(32.dp)
        ApprovedByLogos()
        VSpacer(32.dp)
    }

    if (isPlanSelectBottomSheetVisible) {
        SelectPlanBottomSheet(
            onDismiss = { isPlanSelectBottomSheetVisible = false },
            onPurchase = {
                isPlanSelectBottomSheetVisible = false
                navigation.add(PremiumSubscribedPage)
            }
        )
    }
}

@Composable
private fun TitleText() {
    val premium = stringResource(R.string.Premium_Title)

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        text = highlightText(
            text = stringResource(R.string.Premium_Features, premium),
            textColor = ComposeAppTheme.colors.leah,
            highlightPart = premium,
            highlightColor = ComposeAppTheme.colors.orange
        ),
        style = ComposeAppTheme.typography.title3,
        textAlign = TextAlign.Center,
    )
}

/** What Premium adds. The rows only inform: nothing opens from them. */
@Composable
private fun FeaturesSection(features: List<IPaidAction>) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        features.forEachIndexed { index, feature ->
            if (index > 0) {
                HsDivider()
            }
            CellPrimary(
                left = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(feature.iconRes),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.orange,
                    )
                },
                middle = {
                    CellMiddleInfo(
                        title = stringResource(feature.titleStringRes).hs,
                        subtitle = stringResource(feature.descriptionStringRes).hs,
                    )
                },
            )
        }
    }
}

@Composable
private fun ApprovedByLogos() {
    Row(
        modifier = Modifier
            .padding(horizontal = 25.dp)
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            R.drawable.bitcoin_logo,
            R.drawable.wallet_scrutiny_logo,
            R.drawable.certik_logo,
        ).forEach { logo ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                // Scales down when a third of a narrow screen is less than the logo
                Image(
                    painter = painterResource(logo),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(ComposeAppTheme.colors.leah),
                )
            }
        }
    }
}
