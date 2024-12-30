package cash.p.terminal.featureStacking.ui.staking

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.featureStacking.R
import cash.p.terminal.featureStacking.ui.calculatorScreen.CalculatorViewModel
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.CoinFragmentInput
import org.koin.androidx.viewmodel.ext.android.viewModel

class StackingFragment : BaseComposeFragment() {
    private val viewModel by viewModel<StackingViewModel>()
    private val calculatorViewModel by viewModel<CalculatorViewModel>()

    @Composable
    override fun GetContent(navController: NavController) {
        viewModel.loadData()
        if(calculatorViewModel.uiState.value.calculateResult.isEmpty()) {
            calculatorViewModel.setCalculatorValue("100")
        }
        StackingScreen(
            uiState = viewModel.uiState.value,
            calculatorUIState = calculatorViewModel.uiState.value,
            onCalculatorValueChanged = calculatorViewModel::setCalculatorValue,
            onTabChanged = viewModel::setStackingType,
            onBuyClicked = { token ->
                navController.slideFromRight(R.id.multiswap, token)
            },
            onChartClicked = { coinUid ->
                navController.slideFromRight(R.id.coinFragment, CoinFragmentInput(coinUid))
            },
            onClickClose = navController::popBackStack
        )
    }
}