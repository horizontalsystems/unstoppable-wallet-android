package io.horizontalsystems.core.modules.receive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.core.R
import io.horizontalsystems.core.entities.Wallet
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.serializers.HSScreenKClassSerializer
import io.horizontalsystems.core.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.ui.compose.TranslatableString
import io.horizontalsystems.core.ui.compose.components.HsDivider
import io.horizontalsystems.core.ui.compose.components.MenuItem
import io.horizontalsystems.core.ui.compose.components.VSpacer
import io.horizontalsystems.core.uiv3.components.HSScaffold
import io.horizontalsystems.core.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.core.uiv3.components.cell.CellPrimary
import io.horizontalsystems.core.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.core.uiv3.components.cell.hs
import io.horizontalsystems.core.uiv3.components.info.TextBlock
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class ZcashAddressTypeSelectPage(val input: Input) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val wallet = input.wallet
        ZcashAddressTypeSelectScreen(
            onZcashAddressTypeClick = { isTransparent ->
                navigation.slideFromRight(
                    ReceivePage(ReceivePage.Input(
                        wallet = wallet,
                        receiveEntryPointDestId = input.receiveEntryPointDestId,
                        isTransparentAddress = isTransparent
                    ))
                )
            },
            onBackPress = {
                navigation.removeLastOrNull()
            })
    }

    @Serializable
    data class Input(
        val wallet: Wallet,
        @Serializable(with = HSScreenKClassSerializer::class) val receiveEntryPointDestId: KClass<out HSPage>? = null
    )
}

@Composable
fun ZcashAddressTypeSelectScreen(
    onZcashAddressTypeClick: (Boolean) -> Unit,
    onBackPress: () -> Unit,
    closeModule: (() -> Unit)? = null,
) {
    HSScaffold(
        title = stringResource(R.string.Balance_Receive_AddressType),
        onBack = onBackPress,
        menuItems = if (closeModule == null) emptyList() else
            listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = closeModule
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.lawrence)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .background(ComposeAppTheme.colors.tyler)
                    .fillMaxWidth()
            ) {
                TextBlock(
                    stringResource(R.string.Balance_Receive_AddressTypeZcashDescription)
                )
                VSpacer(20.dp)
            }

            CellPrimary(
                middle = {
                    CellMiddleInfo(
                        title = stringResource(R.string.Balance_Zcash_Shielded).hs,
                        subtitle = stringResource(R.string.Balance_Zcash_ShieldedDescription).hs,
                    )
                },
                right = {
                    CellRightNavigation()
                },
                onClick = {
                    onZcashAddressTypeClick.invoke(false)
                }
            )
            HsDivider()
            CellPrimary(
                middle = {
                    CellMiddleInfo(
                        title = stringResource(R.string.Balance_Zcash_Transparent).hs,
                        subtitle = stringResource(R.string.Balance_Zcash_TransparentDescription).hs,
                    )
                },
                right = {
                    CellRightNavigation()
                },
                onClick = {
                    onZcashAddressTypeClick.invoke(true)
                }
            )
            HsDivider()

            VSpacer(32.dp)
        }
    }
}
