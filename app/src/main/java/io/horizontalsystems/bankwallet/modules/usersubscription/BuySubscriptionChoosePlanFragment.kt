package io.horizontalsystems.bankwallet.modules.usersubscription

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.subscriptions.core.BasePlan
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize

class BuySubscriptionChoosePlanFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            BuySubscriptionChoosePlanScreen(
                navController,
                requireActivity(),
                input
            )
        }
    }

    @Parcelize
    data class Input(val subscriptionId: String) : Parcelable

    @Parcelize
    class Result : Parcelable
}

@Composable
fun BuySubscriptionChoosePlanScreen(
    navController: NavController,
    activity: FragmentActivity,
    input: BuySubscriptionChoosePlanFragment.Input
) {
    val viewModel = viewModel<BuySubscriptionChoosePlanViewModel>(
        factory = BuySubscriptionChoosePlanViewModel.Factory(input.subscriptionId)
    )

    val view = LocalView.current

    val uiState = viewModel.uiState

    LaunchedEffect(uiState.purchase) {
        uiState.purchase?.let { purchase ->
            HudHelper.showSuccessMessage(view, purchase.toString(), SnackbarDuration.LONG)

            delay(300)

            navController.setNavigationResultX(BuySubscriptionChoosePlanFragment.Result())
            navController.popBackStack()
        }
    }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = "Subscriptions",
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            uiState.basePlans.forEach { basePlan ->
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = basePlan.stringRepresentation(),
                    onClick = {
                        viewModel.launchPurchaseFlow(basePlan.id, activity)
                    },
                    enabled = uiState.choosePlanEnabled
                )
                VSpacer(height = 12.dp)
            }

            uiState.error?.let {
                TextImportantWarning(text = it.message ?: it.javaClass.name)
            }
        }
    }
}

fun BasePlan.stringRepresentation(): String {
    return pricingPhases.map {
        "${it.formattedPrice}/${it.billingPeriod}"
    }.joinToString(" then ")
}
