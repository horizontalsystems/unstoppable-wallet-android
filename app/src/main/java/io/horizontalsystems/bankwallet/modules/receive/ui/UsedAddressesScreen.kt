package io.horizontalsystems.bankwallet.modules.receive.ui

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.UsedAddress
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subheadSB_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_leah
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabItem
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsTop
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsTopType
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

@Composable
fun UsedAddressScreen(
    params: UsedAddressesParams,
    onBackPress: () -> Unit
) {
    val tabs = UsedAddressTab.entries
    var selectedTab by remember { mutableStateOf(UsedAddressTab.ReceiveAddress) }
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { tabs.size }

    HSScaffold(
        title = stringResource(R.string.Balance_Receive_UsedAddresses),
        onBack = onBackPress,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.lawrence)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InfoText(
                modifier = Modifier.background(ComposeAppTheme.colors.tyler),
                text = stringResource(
                    id = R.string.Balance_Receive_UsedAddressesDescriptoin,
                    params.coinName
                ),
                paddingBottom = 24.dp
            )

            LaunchedEffect(key1 = selectedTab, block = {
                pagerState.scrollToPage(selectedTab.ordinal)
            })
            val tabItems = tabs.map {
                TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
            }
            TabsTop(TabsTopType.Fitted, tabItems) {
                selectedTab = it
            }

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
    Column {
        usedAddresses.forEach { item ->
            TransactionInfoAddressCell(
                index = item.index.plus(1).toString(),
                address = item.address,
                explorerUrl = item.explorerUrl
            )
            HsDivider()
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
        subheadSB_grey(
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
            text = index
        )

        HSpacer(16.dp)
        subhead_leah(
            modifier = Modifier.weight(1f),
            text = address,
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
