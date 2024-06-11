package io.horizontalsystems.bankwallet.modules.usersubscription

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.subscriptions.core.IPaidAction
import kotlinx.parcelize.Parcelize

class BuySubscriptionFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        BuySubscriptionScreen(navController, requireActivity())
    }

    @Parcelize
    data class Input(val action: IPaidAction) : Parcelable

    @Parcelize
    data class Result(val result: Boolean) : Parcelable
}

@Composable
private fun BuySubscriptionScreen(navController: NavController, activity: FragmentActivity) {
    val viewModel = viewModel<BuySubscriptionViewModel>()

    val uiState = viewModel.uiState

    val plans = uiState.plans

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
            plans.forEach { plan ->
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = plan.name,
                    onClick = {
                        viewModel.launchPurchaseFlow(plan.id, activity)
                    }
                )
                VSpacer(height = 12.dp)
            }
        }
    }
}
