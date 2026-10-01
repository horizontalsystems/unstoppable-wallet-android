package io.horizontalsystems.walletkit.modules.settings.labs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.helpers.LinkHelper
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import kotlinx.serialization.Serializable

@Serializable
data object UnstoppableLabsPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        UnstoppableLabsScreen(onBack = { navigation.removeLastOrNull() })
    }
}

private data class LabsProduct(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
    val link: String,
)

@Composable
private fun UnstoppableLabsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val config = App.appConfigProvider
    val products = listOf(
        LabsProduct(
            R.string.UnstoppableLabs_SimplexSwapBot,
            R.string.UnstoppableLabs_SimplexSwapBot_Description,
            R.drawable.lightning_24,
            config.labsSimplexSwapBotLink,
        ),
        LabsProduct(
            R.string.UnstoppableLabs_TelegramSwapBot,
            R.string.UnstoppableLabs_TelegramSwapBot_Description,
            R.drawable.send_24,
            config.labsTelegramSwapBotLink,
        ),
        LabsProduct(
            R.string.UnstoppableLabs_SignalSwapBot,
            R.string.UnstoppableLabs_SignalSwapBot_Description,
            R.drawable.chat_24,
            config.labsSignalSwapBotLink,
        ),
        LabsProduct(
            R.string.UnstoppableLabs_SwapWeb,
            R.string.UnstoppableLabs_SwapWeb_Description,
            R.drawable.globe_24,
            config.labsSwapWebLink,
        ),
    )

    HSScaffold(
        title = stringResource(R.string.UnstoppableLabs_Title),
        onBack = onBack,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            VSpacer(12.dp)
            CellUniversalLawrenceSection(
                products.map { product ->
                    {
                        CellPrimary(
                            left = {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    painter = painterResource(product.icon),
                                    contentDescription = null,
                                    tint = ComposeAppTheme.colors.grey,
                                )
                            },
                            middle = {
                                CellMiddleInfo(
                                    title = stringResource(product.title).hs,
                                    subtitle = stringResource(product.description).hs,
                                )
                            },
                            right = {
                                CellRightNavigation()
                            },
                            onClick = {
                                LinkHelper.openLinkInAppBrowser(context, product.link)
                            }
                        )
                    }
                }
            )
            VSpacer(36.dp)
        }
    }
}
