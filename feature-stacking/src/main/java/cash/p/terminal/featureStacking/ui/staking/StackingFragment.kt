package cash.p.terminal.featureStacking.ui.staking

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.featureStacking.R
import cash.p.terminal.featureStacking.ui.calculatorScreen.CalculatorViewModel
import cash.p.terminal.navigation.entity.SwapParams
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.CoinFragmentInput
import io.horizontalsystems.core.getInput
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.math.BigDecimal

class StackingFragment : BaseComposeFragment() {
    private val viewModel by viewModel<StackingViewModel>()
    private val calculatorViewModel by viewModel<CalculatorViewModel>()
    private var isSetPreselected: Boolean = false

    @Composable
    override fun GetContent(navController: NavController) {
        if(calculatorViewModel.uiState.value.calculateResult.isEmpty()) {
            calculatorViewModel.setCalculatorValue("10000")
        }

        if(!isSetPreselected) {
            navController.getInput<StackingType>()?.let {
                viewModel.setStackingType(it)
            }
            isSetPreselected = true
        }

        StackingScreen(
            uiState = viewModel.uiState.value,
            calculatorUIState = calculatorViewModel.uiState.value,
            onCalculatorValueChanged = calculatorViewModel::setCalculatorValue,
            onTabChanged = viewModel::setStackingType,
            onBuyClicked = { token ->
                navController.slideFromRight(R.id.multiswap, SwapParams.TOKEN_OUT to token)
            },
            onChartClicked = { coinUid ->
                navController.slideFromRight(R.id.coinFragment, CoinFragmentInput(coinUid))
            },
            onClickClose = navController::popBackStack,
            setCalculatorData = ::setCalculatorData
        )
    }

    private fun setCalculatorData(stackingType: StackingType, balance: BigDecimal) {
        val value = if(stackingType == StackingType.PCASH) {
            if(balance < BigDecimal(100)) {
                "10000"
            } else {
                balance.toPlainString()
            }
        } else {
            if(balance < BigDecimal(1)) {
                "1000"
            } else {
                balance.toPlainString()
            }
        }
        calculatorViewModel.setCalculatorValue(value)
        calculatorViewModel.setCoin(stackingType)
        viewModel.setStackingType(stackingType)
    }
}