package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ConfiguredToken
import io.horizontalsystems.bankwallet.modules.coin.overview.TokenVariants
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun TokenVariants(
    tokenVariants: TokenVariants,
    onClickAddToWallet: (ConfiguredToken) -> Unit,
    onClickCopy: (String) -> Unit,
    onClickExplorer: (String) -> Unit,
) {
    Column {
        CellSingleLineClear(borderTop = true) {
            body_leah(text = stringResource(id = tokenVariants.type.titleResId))
        }

        CellUniversalLawrenceSection(
            items = tokenVariants.items,
            limit = 3
        ) { tokenVariant ->
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Image(
                    modifier = Modifier.size(32.dp),
                    painter = rememberAsyncImagePainter(
                        model = tokenVariant.imgUrl,
                        error = painterResource(R.drawable.ic_platform_placeholder_32)
                    ),
                    contentDescription = "platform"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    tokenVariant.name?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            body_leah(
                                modifier = Modifier.weight(1f, fill = false),
                                text = it,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(1.dp))
                    }
                    subhead2_grey(
                        modifier = Modifier
                            .clickable(
                                enabled = tokenVariant.copyValue != null,
                                onClick = {
                                    onClickCopy.invoke(tokenVariant.copyValue ?: "")
                                }
                            ),
                        text = tokenVariant.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (tokenVariant.canAddToWallet) {
                    if (tokenVariant.inWallet) {
                        ButtonSecondaryCircle(
                            icon = R.drawable.ic_in_wallet_dark_24,
                            contentDescription = stringResource(R.string.CoinPage_InWallet),
                            enabled = false,
                            tint = ComposeAppTheme.colors.grey,
                            onClick = {
//                                HudHelper.showInProcessMessage(view, R.string.Hud_Already_In_Wallet, showProgressBar = false)
                            }
                        )
                    } else {
                        ButtonSecondaryCircle(
                            icon = R.drawable.ic_add_to_wallet_2_24,
                            contentDescription = stringResource(R.string.CoinPage_AddToWallet),
                            onClick = {
                                onClickAddToWallet.invoke(tokenVariant.configuredToken)
                            }
                        )
                    }

                }
                tokenVariant.explorerUrl?.let { explorerUrl ->
                    ButtonSecondaryCircle(
                        modifier = Modifier.padding(start = 16.dp),
                        icon = R.drawable.ic_globe_20,
                        contentDescription = stringResource(R.string.Button_Browser),
                        onClick = {
                            onClickExplorer.invoke(explorerUrl)
                        }
                    )
                }
            }
        }
    }
}