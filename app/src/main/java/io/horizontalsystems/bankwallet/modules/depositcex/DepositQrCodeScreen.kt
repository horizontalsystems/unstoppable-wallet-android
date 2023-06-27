package cash.p.terminal.modules.depositcex

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*
import cash.p.terminal.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun DepositQrCodeScreen(
    onNavigateBack: (() -> Unit)?,
    onClose: () -> Unit,
    cexAsset: CexAsset,
    networkId: String?
) {
    val viewModel =
        viewModel<DepositAddressViewModel>(factory = DepositAddressViewModel.Factory(cexAsset, networkId))

    val uiState = viewModel.uiState

    val address = uiState.address?.address
    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                val navigationIcon: @Composable (() -> Unit)? = onNavigateBack?.let {
                    {
                        HsBackButton(onClick = onNavigateBack)
                    }
                }
                AppBar(
                    title = TranslatableString.PlainString(cexAsset.id),
                    navigationIcon = navigationIcon,
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Done),
                            onClick = onClose
                        )
                    )
                )
            }
        ) {
            Crossfade(targetState = uiState.loading) { loading ->
                Column(modifier = Modifier.padding(it)) {
                    if (loading) {
                        Loading()
                    } else if (address != null) {
                        val qrBitmap = TextHelper.getQrCodeBitmap(address)
                        val view = LocalView.current
                        val context = LocalContext.current

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TextImportantWarning(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                text = "Please make sure that only CoinCode is deposited to this address. Sending other types of tokens to this address will result in their ultimate loss."
                            )
                            VSpacer(40.dp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ComposeAppTheme.colors.white)
                                    .size(231.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                qrBitmap?.let {
                                    Image(
                                        modifier = Modifier
                                            .clickable {
                                                TextHelper.copyText(address)
                                                HudHelper.showSuccessMessage(
                                                    view,
                                                    R.string.Hud_Text_Copied
                                                )
                                            }
                                            .padding(8.dp)
                                            .fillMaxSize(),
                                        bitmap = it.asImageBitmap(),
                                        contentScale = ContentScale.FillWidth,
                                        contentDescription = null
                                    )
                                }
                            }
                            VSpacer(24.dp)
                            subhead2_grey(
                                modifier = Modifier.padding(horizontal = 32.dp),
                                text = "Your address for depositing CoinCode into the Coin Network",
                                textAlign = TextAlign.Center
                            )
                            VSpacer(12.dp)
                            subhead1_leah(
                                text = address,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .clickable {
                                        TextHelper.copyText(address)
                                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                                    },
                            )
                            VSpacer(24.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                ButtonSecondaryDefault(
                                    modifier = Modifier.padding(end = 6.dp),
                                    title = stringResource(R.string.Alert_Copy),
                                    onClick = {
                                        TextHelper.copyText(address)
                                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                                    }
                                )
                                ButtonSecondaryDefault(
                                    modifier = Modifier.padding(start = 6.dp),
                                    title = stringResource(R.string.Deposit_Share),
                                    onClick = {
                                        ShareCompat.IntentBuilder(context)
                                            .setType("text/plain")
                                            .setText(address)
                                            .startChooser()
                                    }
                                )
                            }
                            VSpacer(24.dp)
                        }
                        ButtonsGroupWithShade {
                            ButtonPrimaryYellow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                                title = stringResource(R.string.Button_Close),
                                onClick = onClose,
                            )
                        }
                    } else if (uiState.error != null) {
                        ListEmptyView(
                            text = uiState.error.localizedMessage ?: stringResource(R.string.Error),
                            icon = R.drawable.ic_sync_error
                        )
                    }
                }
            }
        }
    }
}
