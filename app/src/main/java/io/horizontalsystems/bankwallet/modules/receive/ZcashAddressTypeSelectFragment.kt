package io.horizontalsystems.bankwallet.modules.receive

import android.os.Parcelable
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
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import kotlinx.parcelize.Parcelize


class ZcashAddressTypeSelectFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) {
            val wallet = it.wallet
            ZcashAddressTypeSelectScreen(
                onZcashAddressTypeClick = { isTransparent ->
                    navController.slideFromRight(
                        R.id.receiveFragment,
                        ReceiveFragment.Input(
                            wallet = wallet,
                            isTransparentAddress = isTransparent
                        )
                    )
                },
                onBackPress = {
                    navController.popBackStack()
                })
        }
    }

    @Parcelize
    data class Input(val wallet: Wallet) : Parcelable
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
                        title = stringResource(R.string.Balance_Zcash_Unified).hs,
                        subtitle = stringResource(R.string.Balance_Zcash_UnifiedDescription).hs,
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
