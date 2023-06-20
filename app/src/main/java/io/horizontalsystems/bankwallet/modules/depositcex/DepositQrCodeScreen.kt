package io.horizontalsystems.bankwallet.modules.depositcex

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun DepositQrCodeScreen(
    depositViewModel: DepositViewModel,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
) {
    val address = "receive address"
    val qrBitmap = TextHelper.getQrCodeBitmap(address)
    val view = LocalView.current
    val context = LocalContext.current

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.PlainString("Coin Name"),
                    navigationIcon = {
                        HsBackButton(onClick = onNavigateBack)
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Done),
                            onClick = onClose
                        )
                    )
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
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
                                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
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
            }
        }
    }
}
