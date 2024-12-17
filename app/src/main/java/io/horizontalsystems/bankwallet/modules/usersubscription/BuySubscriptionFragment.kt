package io.horizontalsystems.bankwallet.modules.usersubscription

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.core.slideFromRightForResult
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
        BuySubscriptionScreen(navController, navController.requireInput())
    }

    @Parcelize
    data class Input(val action: IPaidAction) : Parcelable

    @Parcelize
    class Result : Parcelable
}

@Composable
private fun BuySubscriptionScreen(
    navController: NavController,
    input: BuySubscriptionFragment.Input,
) {
    val viewModel = viewModel<BuySubscriptionViewModel> {
        BuySubscriptionViewModel(input.action)
    }

    val uiState = viewModel.uiState

    val subscriptions = uiState.subscriptions

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
            subscriptions.forEach { subscription ->
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = subscription.name,
                    onClick = {
                        navController.slideFromRightForResult<BuySubscriptionChoosePlanFragment.Result>(
                            R.id.buySubscriptionChoosePlanFragment,
                            BuySubscriptionChoosePlanFragment.Input(subscription.id)
                        ) {
                            navController.setNavigationResultX(BuySubscriptionFragment.Result())
                            navController.popBackStack()
                        }
                    }
                )
                VSpacer(height = 12.dp)
            }
        }
    }
}
