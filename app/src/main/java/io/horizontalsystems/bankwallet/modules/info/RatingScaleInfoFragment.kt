package io.horizontalsystems.bankwallet.modules.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.Rating
import io.horizontalsystems.bankwallet.modules.info.ui.InfoHeader
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoTextBody
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.core.findNavController

class RatingScaleInfoFragment : BaseFragment() {

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
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                InfoHeader(R.string.Coin_Analytics_RatingScale)
                InfoTextBody(stringResource(R.string.Coin_Analytics_RatingScaleDescription))
                VSpacer(12.dp)
                CellUniversalLawrenceSection(Rating.values().toList(), false) { rating ->
                    val color = when (rating) {
                        Rating.Excellent -> Color(0xFF05C46B)
                        Rating.Good -> Color(0xFFFFA800)
                        Rating.Fair -> Color(0xFFFF7A00)
                        Rating.Poor -> Color(0xFFFF3D00)
                    }
                    RowUniversal(
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Image(
                            painter = painterResource(rating.icon),
                            contentDescription = null
                        )
                        HSpacer(8.dp)
                        Text(
                            text = stringResource(rating.title).uppercase(),
                            style = ComposeAppTheme.typography.subhead1,
                            color = color,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = stringResource(rating.percent),
                            style = ComposeAppTheme.typography.subhead1,
                            color = color,
                        )
                    }
                }
                VSpacer(24.dp)
            }
        }
    }
}