package io.horizontalsystems.walletkit.modules.importwallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.createaccount.WalletTypeGroup
import io.horizontalsystems.walletkit.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.extensions.HSBottomSheet
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetBody
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetHeaderV3
import kotlinx.serialization.Serializable

/**
 * The ways to add an existing wallet as a sheet, for the welcome screen. It
 * also offers a view-only wallet, which the welcome screen has no other way
 * to. Inside the app the list is a page, [ImportWalletPage].
 *
 * @param input where to return once the wallet is added. The sheet closes as
 * soon as it opens something else, so it cannot be the place to return to.
 */
@Serializable
data class ImportWalletSheet(val input: ManageAccountsModule.Input) : HSBottomSheet(expanded = true) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        ImportWalletHost(
            navigation = navigation,
            popUpToInclusiveId = input.popOffOnSuccess,
            inclusive = input.popOffInclusive,
            beforeLeaving = navigation::removeLastOrNull,
        ) { actions ->
            BottomSheetBody {
                BottomSheetHeaderV3(
                    title = stringResource(R.string.ManageAccounts_ImportWallet),
                    onCloseClick = navigation::removeLastOrNull,
                )

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 64.dp),
                ) {
                    ImportWalletWays(
                        actions = actions,
                        iconSize = 32,
                        iconTint = ComposeAppTheme.colors.orange,
                    ) { rows ->
                        WalletTypeGroup { rows() }
                    }
                }
            }
        }
    }
}
