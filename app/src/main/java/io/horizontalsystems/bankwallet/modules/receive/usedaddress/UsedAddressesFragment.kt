package io.horizontalsystems.bankwallet.modules.receive.usedaddress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.parcelable

class UsedAddressesFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val viewItem = requireArguments().parcelable<UsedAddressesViewItem>(USED_ADDRESSES_VIEW_ITEM)!!
        UsedAddressScreen(navController, viewItem)
    }

    companion object {
        private const val USED_ADDRESSES_VIEW_ITEM = "used_addresses_view_item"
        fun prepareParams(item: UsedAddressesViewItem) = bundleOf(USED_ADDRESSES_VIEW_ITEM to item)
    }
}

@Composable
fun UsedAddressScreen(
    navController: NavController,
    viewItem: UsedAddressesViewItem
) {
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(id = R.string.Balance_Receive_UsedAddresses),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }

            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            InfoText(text = stringResource(id = R.string.Balance_Receive_UsedAddressesDescriptoin, viewItem.coinName))
            Spacer(Modifier.height(12.dp))

            CellUniversalLawrenceSection(
                buildList {
                    for (item in viewItem.usedAddresses)
                        add {
                            TransactionInfoAddressCell(index = item.index.toString(), address = item.address, explorerUrl = item.explorerUrl)
                        }
                }
            )

            VSpacer(24.dp)
        }
    }
}

@Composable
fun TransactionInfoAddressCell(
    index: String,
    address: String,
    explorerUrl: String
) {
    val view = LocalView.current
    val context = LocalContext.current
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        subhead2_grey(text = index)

        HSpacer(16.dp)
        subhead2_leah(
            modifier = Modifier.weight(1f),
            text = address,
            textAlign = TextAlign.Right
        )

        HSpacer(16.dp)
        ButtonSecondaryCircle(
            icon = R.drawable.ic_language,
            onClick = { LinkHelper.openLinkInAppBrowser(context, explorerUrl) }
        )

        HSpacer(16.dp)
        ButtonSecondaryCircle(
            icon = R.drawable.ic_copy_20,
            onClick = {
                TextHelper.copyText(address)
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
            }
        )
    }
}
