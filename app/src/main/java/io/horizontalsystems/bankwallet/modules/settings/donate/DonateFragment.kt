package io.horizontalsystems.bankwallet.modules.settings.donate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class DonateFragment : BaseFragment() {

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
                    DonateScreen(
                        onBackPress = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun DonateScreen(
    onBackPress: () -> Unit
) {
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.ResString(R.string.Settings_Donate),
                navigationIcon = {
                    HsBackButton(onClick = onBackPress)
                },
            )
        }
    ) {
        Column(Modifier.padding(it)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(12.dp)
                HeartBlock()
                VSpacer(24.dp)
                DonateAddress(
                    coinImageUrl = "https://cdn.blocksdecoded.com/blockchain-icons/32px/bitcoin@3x.png",
                    coinName = "Bitcoin",
                    address = "bc1qw5tw4cnyt0vxts70ntdzxesn2zzz97t6r29pjj"
                )
                VSpacer(24.dp)
                DonateAddress(
                    coinImageUrl = "https://cdn.blocksdecoded.com/blockchain-icons/32px/ethereum@3x.png",
                    coinName = "Ethereum",
                    address = "0x8a2Bec907827F496752c3F24F960B3cddc5D311B"
                )
                VSpacer(24.dp)
                DonateAddress(
                    coinImageUrl = "https://cdn.blocksdecoded.com/blockchain-icons/32px/binance-smart-chain@3x.png",
                    coinName = "BNB Smart Chain",
                    address = "0x8a2Bec907827F496752c3F24F960B3cddc5D311B"
                )
                VSpacer(32.dp)
            }
        }
    }
}

@Composable
private fun DonateAddress(
    coinImageUrl: String,
    coinName: String,
    address: String
) {
    val localView = LocalView.current

    InfoText(text = stringResource(R.string.Settings_Donate_CoinAddress, coinName).uppercase())
    CellUniversalLawrenceSection() {
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = {
                TextHelper.copyText(address)
                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
            }
        ) {
            Image(
                modifier = Modifier.size(32.dp),
                painter = rememberAsyncImagePainter(
                    model = coinImageUrl,
                    error = painterResource(R.drawable.ic_platform_placeholder_32)
                ),
                contentDescription = "platform"
            )
            HSpacer(16.dp)
            subhead2_leah(
                modifier = Modifier.weight(1f),
                text = address,
            )

            HSpacer(16.dp)
            ButtonSecondaryCircle(
                icon = R.drawable.ic_copy_20,
                contentDescription = stringResource(R.string.Button_Copy),
                onClick = {
                    TextHelper.copyText(address)
                    HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                }
            )
        }
    }
}

@Composable
private fun HeartBlock() {
    CellUniversalLawrenceSection(
        listOf {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VSpacer(32.dp)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = ComposeAppTheme.colors.steel10,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        painter = painterResource(R.drawable.ic_heart_48),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.jacob
                    )
                }
                VSpacer(32.dp)
                headline2_leah(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.Settings_Donate_Info),
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
                VSpacer(32.dp)
            }
        }
    )
}

@Preview
@Composable
fun DonateScreenPreview() {
    ComposeAppTheme {
        DonateScreen {}
    }
}