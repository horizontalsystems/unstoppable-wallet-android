package com.quantum.wallet.bankwallet.modules.xtransaction.cells

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quantum.wallet.bankwallet.ui.compose.components.HSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.cell.CellUniversal
import com.quantum.wallet.bankwallet.ui.compose.components.subhead1_leah
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_grey

@Composable
fun TitleAndValueCell(
    title: String,
    value: String,
    borderTop: Boolean = true
) {
    CellUniversal(borderTop = borderTop) {
        subhead2_grey(text = title, modifier = Modifier.padding(end = 16.dp))
        HSpacer(16.dp)
        subhead1_leah(
            modifier = Modifier.weight(1f),
            text = value,
            textAlign = TextAlign.Right
        )
    }
}