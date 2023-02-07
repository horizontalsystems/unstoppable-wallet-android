package cash.p.terminal.modules.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.info.ui.InfoBody
import cash.p.terminal.modules.info.ui.InfoHeader
import cash.p.terminal.modules.info.ui.InfoSubHeader
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.MenuItem
import io.horizontalsystems.core.findNavController

class TokenDistributionInfoFragment : BaseFragment() {

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
                    InfoScreen(
                        findNavController()
                    )
                }
            }
        }
    }

}

@Composable
private fun InfoScreen(
    navController: NavController
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                InfoHeader(R.string.CoinPage_TokenDistribution)
                InfoBody(R.string.CoinPage_TokenDistribution_Description)
                InfoSubHeader(R.string.CoinPage_DetailsTxCount)
                InfoBody(R.string.CoinPage_DetailsTxCount_Description)
                InfoSubHeader(R.string.CoinPage_DetailsTxVolume)
                InfoBody(R.string.CoinPage_DetailsTxVolume_Description)
                InfoSubHeader(R.string.CoinPage_DetailsActiveAddresses)
                InfoBody(R.string.CoinPage_DetailsActiveAddresses_Description)
                InfoSubHeader(R.string.CoinPage_MajorHolders)
                InfoBody(R.string.CoinPage_MajorHolders_InfoDescription)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
