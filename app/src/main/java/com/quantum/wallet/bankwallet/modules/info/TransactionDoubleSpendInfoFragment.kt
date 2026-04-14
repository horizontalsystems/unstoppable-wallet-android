package com.quantum.wallet.bankwallet.modules.info

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.shorten
import com.quantum.wallet.bankwallet.modules.info.ui.InfoHeader
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonSecondaryDefault
import com.quantum.wallet.bankwallet.ui.compose.components.CellSingleLineLawrence
import com.quantum.wallet.bankwallet.ui.compose.components.HsDivider
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.TextImportantWarning
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_grey
import com.quantum.wallet.bankwallet.ui.helpers.TextHelper
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize

class TransactionDoubleSpendInfoFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            InfoScreen(
                txHash = input.transactionHash,
                conflictingTxHash = input.conflictingTransactionHash,
                onBackClick = { navController.popBackStack() }
            )
        }
    }

    @Parcelize
    data class Input(
        val transactionHash: String,
        val conflictingTransactionHash: String,
    ) : Parcelable
}

@Composable
private fun InfoScreen(
    txHash: String,
    conflictingTxHash: String,
    onBackClick: () -> Unit
) {
    HSScaffold(
        title = "",
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = onBackClick
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            InfoHeader(R.string.Info_DoubleSpend_Title)
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                text = stringResource(R.string.Info_DoubleSpend_Description),
            )
            ConflictingTransactions(txHash, conflictingTxHash)
            VSpacer(44.dp)
        }
    }
}

@Composable
fun ConflictingTransactions(transactionHash: String, conflictingHash: String) {
    VSpacer(12.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        TransactionHashCell(R.string.Info_DoubleSpend_ThisTx, transactionHash)
        HsDivider(modifier = Modifier.fillMaxWidth())
        TransactionHashCell(R.string.Info_DoubleSpend_ConflictingTx, conflictingHash)
    }
}

@Composable
private fun TransactionHashCell(titleRes: Int, transactionHash: String) {
    val view = LocalView.current
    CellSingleLineLawrence() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            subhead2_grey(
                text = stringResource(titleRes),
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            )
            ButtonSecondaryDefault(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = transactionHash.shorten(),
                onClick = {
                    TextHelper.copyText(transactionHash)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                }
            )
        }
    }
}

@Preview
@Composable
private fun Preview_InfoScreen() {
    ComposeAppTheme {
        InfoScreen(
            "jh2rnj23rnk2b3k42b2k4jb",
            "nb3k4brk34bk34bk34bk3g"
        ) { }
    }
}