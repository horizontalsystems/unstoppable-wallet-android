package io.horizontalsystems.bankwallet.modules.settings.subscription

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatPremiumTrigger
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class SubscriptionFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        SubscriptionScreen(navController)
    }

}

@Composable
fun SubscriptionScreen(navController: NavController) {
    val viewModel = viewModel<SubscriptionViewModel>()

    val uiState = viewModel.uiState
    val context = LocalContext.current

    HSScaffold(
        title = stringResource(R.string.Settings_Subscription),
        onBack = navController::popBackStack,
    ) {
        Column {
            VSpacer(12.dp)

            SectionUniversalLawrence {
                if (uiState.userHasActiveSubscription) {
                    CellUniversal(
                        borderTop = false,
                        onClick = {
                            viewModel.launchManageSubscriptionScreen(context)
                        }
                    ) {
                        body_leah(
                            text = stringResource(R.string.SettingsSubscription_ManageSubscription),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                        )
                    }
                } else {
                    CellUniversal(
                        borderTop = false,
                        onClick = {
                            navController.slideFromBottom(R.id.buySubscriptionFragment)
                            stat(
                                page = StatPage.PurchaseList,
                                event = StatEvent.OpenPremium(StatPremiumTrigger.GetPremium)
                            )
                        }
                    ) {
                        body_leah(
                            text = stringResource(R.string.SettingsSubscription_GetPremium),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                        )
                    }
                    CellUniversal(
                        onClick = viewModel::restorePurchase
                    ) {
                        body_jacob(
                            text = stringResource(R.string.SettingsSubscription_RestorePurchase),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}
