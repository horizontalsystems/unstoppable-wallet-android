package io.horizontalsystems.walletkit.modules.createaccount

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.extensions.HSBottomSheet
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetBody
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetHeaderV3
import kotlinx.serialization.Serializable

/**
 * The wallet type choice as a sheet, for the welcome screen. Inside the app
 * the same choice is a page, [CreateAccountPage].
 *
 * @param input where to return once the wallet is created. The sheet closes
 * as soon as a type is chosen, so it cannot be the place to return to.
 */
@Serializable
data class CreateAccountSheet(val input: ManageAccountsModule.Input) : HSBottomSheet(expanded = true) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        BottomSheetBody {
            BottomSheetHeaderV3(
                title = stringResource(R.string.ManageAccounts_CreateNewWallet),
                onCloseClick = navigation::removeLastOrNull,
            )

            // Large text or a short screen can leave the rows taller than the sheet
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 64.dp)
            ) {
                WalletTypeGroup {
                    WalletTypes(
                        iconSize = 32,
                        iconTint = ComposeAppTheme.colors.orange,
                        onPasskey = {
                            navigation.removeLastOrNull()
                            navigation.openPasskeyWallet(input)
                        },
                        onStandard = {
                            navigation.removeLastOrNull()
                            navigation.slideFromRight(CreateAccountStandardPage(input))
                        },
                    )
                }
            }
        }
    }
}

/** The bordered group that holds the rows of a wallet type sheet. */
@Composable
fun WalletTypeGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, ComposeAppTheme.colors.blade, shape),
        content = content,
    )
}
