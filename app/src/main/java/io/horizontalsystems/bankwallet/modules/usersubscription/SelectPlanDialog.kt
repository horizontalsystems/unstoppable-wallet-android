package io.horizontalsystems.bankwallet.modules.usersubscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.ButtonsGroupHorizontal
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
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
    viewModel: BuySubscriptionChoosePlanViewModel = viewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
    BottomSheetContent(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) { snackbarActions ->
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
                onCloseClick = onDismiss
            )
            VSpacer(8.dp)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .border(0.5.dp, ComposeAppTheme.colors.blade, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
            ) {
                uiState.basePlans.forEachIndexed { index, basePlan ->
                    if (index > 0) {
                        HsDivider()
                    }
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
            VSpacer(12.dp)

            val buttonTitle = if (freeTrialPeriodDays != null) {
                stringResource(R.string.Premium_GetFreePeriod, freeTrialPeriodDays)
            } else {
                stringResource(R.string.Premium_Subscribe)
            }

            ButtonsGroupHorizontal {
                HSButton(
                    title = buttonTitle,
                    size = ButtonSize.Medium,
                    modifier = Modifier
                        .fillMaxWidth(),
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
            VSpacer(8.dp)
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
    CellPrimary(
        left = {
            HsCheckbox(
                checked = isSelected
            ) {
                onClick()
            }
        },
        middle = {
            CellMiddleInfo(
                title = title.hs,
                badge = badgeText?.hs(color = ComposeAppTheme.colors.remus),
                subtitle = price.hs(color = ComposeAppTheme.colors.jacob),
                subtitle2 = note.hs(color = ComposeAppTheme.colors.remus)
            )
        },
        onClick = onClick
    )
}