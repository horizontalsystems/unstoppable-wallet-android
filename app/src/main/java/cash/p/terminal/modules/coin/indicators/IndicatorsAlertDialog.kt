package cash.p.terminal.modules.coin.indicators

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui.extensions.BaseComposableBottomSheetFragment
import cash.p.terminal.ui.extensions.BottomSheetHeader
import cash.p.terminal.ui_compose.findNavController

class IndicatorsAlertDialog : BaseComposableBottomSheetFragment() {

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
                IndicatorsAlertScreen(findNavController())
            }
        }
    }

}

@Composable
private fun IndicatorsAlertScreen(navController: NavController) {
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.icon_24_lock),
            iconTint = ColorFilter.tint(cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.grey),
            title = stringResource(R.string.CoinPage_Indicators),
            onCloseClick = {
                navController.popBackStack()
            }
        ) {
            InfoText(
                text = stringResource(R.string.CoinPage_IndicatorsAlertText)
            )
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                title = stringResource(R.string.Button_LearnMore),
                onClick = {

                },
            )
            VSpacer(32.dp)
        }
    }
}
