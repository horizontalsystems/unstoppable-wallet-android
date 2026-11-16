package io.horizontalsystems.walletkit.modules.usersubscription

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.designkit.components.HSButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.subscriptions.core.HSPurchase
import io.horizontalsystems.subscriptions.core.numberOfDays
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.modules.usersubscription.BuySubscriptionModel.badge
import io.horizontalsystems.walletkit.modules.usersubscription.BuySubscriptionModel.note
import io.horizontalsystems.walletkit.modules.usersubscription.BuySubscriptionModel.price
import io.horizontalsystems.walletkit.modules.usersubscription.BuySubscriptionModel.title
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.helpers.LinkHelper
import io.horizontalsystems.walletkit.uiv3.TestTags
import io.horizontalsystems.walletkit.uiv3.components.bottombars.ButtonsGroupVertical
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.SnackbarActions
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.HSSelector
import io.horizontalsystems.walletkit.uiv3.components.text.textWithLinks

/** The "Select subscription" sheet, shown over the Premium page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPlanBottomSheet(
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
) {
    BottomSheetContent(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) { snackbarActions ->
        SelectPlanContent(onPurchase, snackbarActions)
    }
}

@Composable
private fun SelectPlanContent(
    onPurchase: () -> Unit,
    snackbarActions: SnackbarActions,
    viewModel: BuySubscriptionChoosePlanViewModel = viewModel(),
) {
    val uiState = viewModel.uiState
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        viewModel.getBasePlans()
    }

    uiState.purchase?.let {
        if (it.status == HSPurchase.Status.Purchased) {
            onPurchase()
        }
    }

    val selectedItemIndex = uiState.selectedIndex
    val freeTrialPeriodDays = uiState.freeTrialPeriod?.let {
        stringResource(R.string.Period_Days, it.numberOfDays())
    }
    uiState.error?.let {
        snackbarActions.showErrorMessage(it.message ?: "Error")
        viewModel.onErrorHandled()
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BottomSheetHeaderV3(
            title = stringResource(R.string.Premium_SelectSubscription),
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            uiState.basePlans.forEachIndexed { index, basePlan ->
                if (index > 0) {
                    HsDivider()
                }
                SubscriptionOption(
                    modifier = Modifier.testTag(TestTags.premiumPlan(basePlan.id)),
                    title = basePlan.title(),
                    price = basePlan.price(),
                    note = basePlan.note(),
                    badge = basePlan.badge(),
                    isSelected = selectedItemIndex == index,
                    onClick = {
                        viewModel.select(index)
                    }
                )
            }
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, top = 12.dp, end = 32.dp, bottom = 24.dp)
                .testTag(TestTags.PREMIUM_PLANS_AGREEMENT),
            text = agreementText(freeTrialPeriodDays),
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead,
            textAlign = TextAlign.Center,
        )

        ButtonsGroupVertical {
            HSButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.PREMIUM_PLANS_SUBSCRIBE),
                title = if (freeTrialPeriodDays != null) {
                    stringResource(R.string.Premium_GetFreePeriod, freeTrialPeriodDays)
                } else {
                    stringResource(R.string.Premium_Subscribe)
                },
                onClick = {
                    activity?.let { activity ->
                        uiState.subscriptionId?.let { subscriptionId ->
                            viewModel.launchPurchaseFlow(
                                subscriptionId = subscriptionId,
                                offerToken = uiState.basePlans[selectedItemIndex].offerToken,
                                activity = activity
                            )
                        }
                    }
                }
            )
        }
    }
}

/**
 * The line above the button: the free period when the plan has one, that the
 * subscription can be cancelled, and the agreement with its two links.
 */
@Composable
private fun agreementText(freeTrialPeriodDays: String?): AnnotatedString {
    val context = LocalContext.current
    val terms = stringResource(R.string.Premium_Agreement_Terms)
    val privacy = stringResource(R.string.Premium_Agreement_Privacy)
    val agreement = textWithLinks(
        sentence = stringResource(R.string.Premium_Agreement, terms, privacy),
        links = listOf(
            terms to {
                LinkHelper.openLinkInAppBrowser(context, App.appConfigProvider.termsOfUseLink)
                stat(page = StatPage.PurchaseSelector, event = StatEvent.Open(StatPage.Terms))
            },
            privacy to {
                LinkHelper.openLinkInAppBrowser(context, App.appConfigProvider.privacyPolicyLink)
                stat(page = StatPage.PurchaseSelector, event = StatEvent.Open(StatPage.Privacy))
            },
        ),
        styles = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline)),
    )

    return buildAnnotatedString {
        freeTrialPeriodDays?.let {
            withStyle(SpanStyle(color = ComposeAppTheme.colors.remus)) {
                append(stringResource(R.string.Premium_EnjoyFreePeriod, it))
            }
            append(" ")
        }
        append(stringResource(R.string.Premium_CancelAnytime))
        append(" ")
        append(agreement)
    }
}

@Composable
private fun SubscriptionOption(
    modifier: Modifier,
    title: String,
    price: String,
    note: String?,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    CellPrimary(
        modifier = modifier,
        left = {
            HSSelector(
                checked = isSelected
            ) {
                onClick()
            }
        },
        middle = {
            CellMiddleInfo(
                title = title.hs,
                badge = badge?.hs(color = ComposeAppTheme.colors.remus),
                subtitle = price.hs,
                subtitle2 = note?.hs(color = ComposeAppTheme.colors.remus),
            )
        },
        onClick = onClick
    )
}
