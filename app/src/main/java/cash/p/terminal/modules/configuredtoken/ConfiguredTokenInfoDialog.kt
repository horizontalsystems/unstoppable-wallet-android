package cash.p.terminal.modules.configuredtoken

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import cash.p.terminal.R
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.iconUrl
import cash.p.terminal.core.imageUrl
import cash.p.terminal.entities.ConfiguredToken
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.*
import cash.p.terminal.ui.extensions.BaseComposableBottomSheetFragment
import cash.p.terminal.ui.extensions.BottomSheetHeader
import cash.p.terminal.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType

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
    val context = LocalContext.current
    val token = configuredToken.token
    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = ImageSource.Remote(token.coin.iconUrl, token.iconPlaceholder).painter(),
            title = token.coin.code,
            subtitle = token.coin.name,
            onCloseClick = { navController.popBackStack() }
        ) {
            when (token.blockchainType) {
                BlockchainType.Bitcoin,
                BlockchainType.Litecoin -> {
                    body_leah(
                        text = stringResource(id = R.string.ManageCoins_BipsDescription),
                        modifier = Modifier.padding(start = 32.dp, top = 12.dp, end = 32.dp, bottom = 24.dp)
                    )
                }
                BlockchainType.BitcoinCash -> {
                    body_leah(
                        text = stringResource(id = R.string.ManageCoins_BchTypeDescription),
                        modifier = Modifier.padding(start = 32.dp, top = 12.dp, end = 32.dp, bottom = 24.dp)
                    )
                }
                else -> {
                    when (val type = token.type) {
                        is TokenType.Eip20 -> {
                            InfoText(text = stringResource(id = R.string.ManageCoins_ContractAddress))

                            CellUniversalLawrenceSection(showFrame = true) {
                                RowUniversal(
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Image(
                                        modifier = Modifier.size(32.dp),
                                        painter = rememberAsyncImagePainter(
                                            model = token.blockchain.type.imageUrl,
                                            error = painterResource(R.drawable.ic_platform_placeholder_32)
                                        ),
                                        contentDescription = "platform"
                                    )
                                    HSpacer(16.dp)
                                    subhead2_leah(
                                        modifier = Modifier.weight(1f),
                                        text = type.address,
                                    )
                                    val explorerUrl = token.blockchain.explorerUrl?.replace("\$ref", type.address)

                                    explorerUrl?.let {
                                        HSpacer(16.dp)
                                        ButtonSecondaryCircle(
                                            icon = R.drawable.ic_globe_20,
                                            contentDescription = stringResource(R.string.Button_Browser),
                                            onClick = {
                                                LinkHelper.openLinkInAppBrowser(context, it)
                                            }
                                        )
                                    }
                                }
                            }

                            VSpacer(24.dp)
                        }
                        TokenType.Native -> TODO()
                        is TokenType.Bep2 -> TODO()
                        is TokenType.Spl -> TODO()
                        is TokenType.Unsupported -> TODO()
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
