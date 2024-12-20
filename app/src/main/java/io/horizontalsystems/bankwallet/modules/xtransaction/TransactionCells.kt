package io.horizontalsystems.bankwallet.modules.xtransaction

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import java.math.BigDecimal

@Composable
fun XxxSectionHeaderCell(
    title: String,
    value: String,
    borderTop: Boolean = true,
    painter: Painter?
) {
    CellUniversal(borderTop = borderTop) {
        painter?.let {
            Icon(
                modifier = Modifier.padding(end = 16.dp),
                painter = painter,
                tint = ComposeAppTheme.colors.grey,
                contentDescription = null,
            )
        }

        body_leah(text = title)

        subhead1_grey(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = value,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun XxxTitleAndValueCell(
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

@Composable
fun xxxCoinAmount(value: BigDecimal?, coinCode: String, sign: String): String {
//    if (hideAmount) return "*****"
    if (value == null) return "---"

    return sign + App.numberFormatter.formatCoinFull(value, coinCode, 8)
}

@Composable
fun xxxFiatAmount(value: BigDecimal?, fiatSymbol: String): String {
//    if (hideAmount) return "*****"
    if (value == null) return "---"

    return App.numberFormatter.formatFiatFull(value, fiatSymbol)
}