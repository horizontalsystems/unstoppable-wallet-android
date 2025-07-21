package cash.p.terminal.modules.receive.ui

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.wallet.entities.UsedAddress
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.TabItem
import cash.p.terminal.ui_compose.components.Tabs
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui.helpers.LinkHelper
import cash.p.terminal.ui.helpers.TextHelper
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize

@Parcelize
data class UsedAddressesParams(
    val coinName: String,
    val usedAddresses: List<UsedAddress>,
    val usedChangeAddresses: List<UsedAddress>
) : Parcelable

enum class UsedAddressTab(@StringRes val titleResId: Int) {
    ReceiveAddress(R.string.Balance_Receive_ReceiveAddresses),
    ChangeAddress(R.string.Balance_Receive_ChangeAddresses);
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UsedAddressScreen(
    params: UsedAddressesParams,
    onBackPress: () -> Unit
) {
    val tabs = UsedAddressTab.values()
    var selectedTab by remember { mutableStateOf(UsedAddressTab.ReceiveAddress) }
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { tabs.size }

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(id = R.string.Balance_Receive_UsedAddresses),
                navigationIcon = {
                    HsBackButton(onClick = onBackPress)
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

            InfoText(text = stringResource(id = R.string.Balance_Receive_UsedAddressesDescriptoin, params.coinName))

            VSpacer(12.dp)

            LaunchedEffect(key1 = selectedTab, block = {
                pagerState.scrollToPage(selectedTab.ordinal)
            })
            val tabItems = tabs.map {
                TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
            }
            Tabs(tabItems, onClick = { selectedTab = it })

            VSpacer(12.dp)

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false
            ) { page ->
                when (tabs[page]) {
                    UsedAddressTab.ReceiveAddress -> AddressList(params.usedAddresses)

                    UsedAddressTab.ChangeAddress -> AddressList(params.usedChangeAddresses)
                }
            }

            VSpacer(24.dp)
        }
    }
}

@Composable
private fun AddressList(usedAddresses: List<UsedAddress>) {
    CellUniversalLawrenceSection(
        buildList {
            for (item in usedAddresses)
                add {
                    TransactionInfoAddressCell(
                        index = item.index.plus(1).toString(),
                        address = item.address,
                        explorerUrl = item.explorerUrl
                    )
                }
        }
    )
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
