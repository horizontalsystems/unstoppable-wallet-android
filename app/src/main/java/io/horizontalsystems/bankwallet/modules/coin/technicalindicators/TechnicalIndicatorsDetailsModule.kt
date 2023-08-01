package cash.p.terminal.modules.coin.technicalindicators

import androidx.compose.ui.graphics.Color

object TechnicalIndicatorsDetailsModule {

    data class SectionViewItem(
        val title: String,
        val details: List<DetailViewItem>,
    )

    data class DetailViewItem(
        val title: String,
        val advice: AdviceViewType,
    )
}

data class TechnicalIndicatorData(
    val title: String,
    val value: AdviceViewType,
    val blocks: List<AdviceBlock>
)

data class AdviceBlock(
    val type: AdviceViewType,
    val filled: Boolean
)

enum class AdviceType {
    SELL, NEUTRAL, BUY, NODATA
}

enum class AdviceViewType {
    STRONGSELL, SELL, NEUTRAL, BUY, STRONGBUY;

    val color: Color
        get() = when (this) {
            STRONGSELL -> Color(0xFFF43A4F)
            SELL -> Color(0xFFF4503A)
            NEUTRAL -> Color(0xFFFFA800)
            BUY -> Color(0xFFB5C405)
            STRONGBUY -> Color(0xFF05C46B)
        }

    val color20: Color
        get() = when (this) {
            STRONGSELL -> Color(0x33F43A4F)
            SELL -> Color(0x33F4503A)
            NEUTRAL -> Color(0x33FFA800)
            BUY -> Color(0x33B5C405)
            STRONGBUY -> Color(0x3305C46B)
        }

    val title: String
        get() = when (this) {
            STRONGSELL -> "Strong Sell"
            SELL -> "Sell"
            NEUTRAL -> "Neutral"
            BUY -> "Buy"
            STRONGBUY -> "Strong Buy"
        }
}