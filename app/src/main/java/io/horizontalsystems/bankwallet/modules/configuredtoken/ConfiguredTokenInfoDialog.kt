package cash.p.terminal.modules.configuredtoken

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.iconUrl
import cash.p.terminal.entities.ConfiguredToken
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.extensions.BaseComposableBottomSheetFragment
import cash.p.terminal.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController

class ConfiguredTokenInfoDialog : BaseComposableBottomSheetFragment() {

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
                val configuredToken = arguments?.getParcelable<ConfiguredToken>(configuredTokenKey)
                if (configuredToken != null) {
                    ConfiguredTokenInfo(findNavController(), configuredToken)
                }
            }
        }
    }

    companion object {
        private const val configuredTokenKey = "configuredToken"

        fun prepareParams(configuredToken: ConfiguredToken): Bundle {
            return bundleOf(configuredTokenKey to configuredToken)
        }
    }
}

@Composable
private fun ConfiguredTokenInfo(navController: NavController, configuredToken: ConfiguredToken) {
    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = ImageSource.Remote(configuredToken.token.coin.iconUrl, configuredToken.token.iconPlaceholder).painter(),
            title = configuredToken.token.coin.code,
            subtitle = configuredToken.token.coin.name,
            onCloseClick = { navController.popBackStack() }
        ) {
            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.height(44.dp))
        }
    }
}
