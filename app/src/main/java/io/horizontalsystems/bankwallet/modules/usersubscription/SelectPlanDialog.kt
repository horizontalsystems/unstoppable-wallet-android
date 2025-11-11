package io.horizontalsystems.bankwallet.modules.usersubscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionModel.badge
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionModel.stringRepresentation
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionModel.title
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_remus
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.subscriptions.core.HSPurchase
import io.horizontalsystems.subscriptions.core.numberOfDays

class SelectPlanDialog : BaseComposableBottomSheetFragment() {

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
                    SelectPlanBottomSheet(
                        onDismiss = { navController.popBackStack() },
                        onPurchase = {
                            navController.popBackStack()
                            navController.slideFromBottom(R.id.premiumSubscribedDialog)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPlanBottomSheet(
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BottomSheetContent(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) { snackbarActions ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SelectSubscriptionBottomSheet(
                onDismiss = onDismiss,
                onPurchase = onPurchase,
                onError = {
                    snackbarActions.showErrorMessage(it.message ?: "Error")
                }
            )
        }
    }
}

@Composable
fun SelectSubscriptionBottomSheet(
    onDismiss: () -> Unit,
    viewModel: BuySubscriptionChoosePlanViewModel = viewModel(),
    onPurchase: () -> Unit,
    onError: (Throwable) -> Unit,
) {
    val uiState = viewModel.uiState
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        viewModel.getBasePlans()
    }

    uiState.error?.let {
        onError(it)
        viewModel.onErrorHandled()
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

    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_circle_clock_24),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
        title = stringResource(R.string.Premium_SelectSubscription),
        onCloseClick = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                uiState.basePlans.forEachIndexed { index, basePlan ->
                    SubscriptionOption(
                        title = basePlan.title(),
                        price = basePlan.stringRepresentation(),
                        note = "",
                        isSelected = selectedItemIndex == index,
                        badgeText = basePlan.badge(),
                        onClick = {
                            viewModel.select(index)
                        }
                    )
                }
            }

            val bottomText = if (freeTrialPeriodDays != null) {
                buildAnnotatedString {
                    withStyle(SpanStyle(color = ComposeAppTheme.colors.remus)) {
                        append(
                            text = stringResource(
                                R.string.Premium_EnjoyFreePeriod,
                                freeTrialPeriodDays
                            )
                        )
                    }
                    append(" ")
                    withStyle(SpanStyle(color = ComposeAppTheme.colors.leah)) {
                        append(text = stringResource(R.string.Premium_CancelSubscriptionInfo))
                    }
                }
            } else {
                buildAnnotatedString {
                    withStyle(SpanStyle(color = ComposeAppTheme.colors.leah)) {
                        append(text = stringResource(R.string.Premium_CancelSubscriptionInfo))
                    }
                }
            }


            VSpacer(12.dp)
            Text(
                text = bottomText,
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subheadR,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
            )
            VSpacer(24.dp)

            val buttonTitle = if (freeTrialPeriodDays != null) {
                stringResource(R.string.Premium_GetFreePeriod, freeTrialPeriodDays)
            } else {
                stringResource(R.string.Premium_Subscribe)
            }
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = buttonTitle,
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
            VSpacer(36.dp)
        }
    }
}

@Composable
fun SubscriptionOption(
    title: String,
    price: String,
    note: String,
    isSelected: Boolean,
    badgeText: String?,
    onClick: () -> Unit
) {
    val borderColor =
        if (isSelected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.blade

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, borderColor, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                headline1_leah(title)
                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                ComposeAppTheme.colors.remus,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = ComposeAppTheme.colors.blade,
                            style = ComposeAppTheme.typography.microSB,
                        )
                    }
                }
            }

            Row() {
                subhead2_jacob(price)
                if (note.isNotEmpty()) {
                    HSpacer(4.dp)
                    subhead2_remus(note)
                }
            }
        }
    }
}