package io.horizontalsystems.bankwallet.modules.receive.monero

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.captionSB_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subheadSB_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_leah
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubaddressesParams(
    val subaddresses: List<SubaddressViewItem>
) : Parcelable

@Composable
fun MoneroSubaddressesScreen(
    params: SubaddressesParams,
    onBackPress: () -> Unit
) {
    HSScaffold(
        title = stringResource(R.string.Balance_Receive_Subaddresses),
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
                text = stringResource(R.string.Balance_Receive_SubaddressesInfo),
                paddingBottom = 24.dp
            )

            AddressList(params.subaddresses)

            VSpacer(36.dp)
        }
    }
}

@Composable
private fun AddressList(subaddresses: List<SubaddressViewItem>) {
    Column {
        subaddresses.forEach { item ->
            TransactionInfoAddressCell(
                index = item.index.toString(),
                address = item.address,
                transactionsCount = item.transactions
            )
            HsDivider()
        }
    }
}

@Composable
fun TransactionInfoAddressCell(
    index: String,
    address: String,
    transactionsCount: Int
) {
    val view = LocalView.current
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        subheadSB_grey(
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
            text = index
        )

        HSpacer(16.dp)

        Column(modifier = Modifier.weight(1f)) {
            subhead_leah(
                text = address,
            )
            VSpacer(2.dp)
            captionSB_grey(
                text = stringResource(
                    R.string.Balance_Receive_SubaddressesTransactions,
                    transactionsCount
                ),
                maxLines = 1
            )
        }

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
