package bitcoin.wallet.viewHelpers

import java.text.NumberFormat

object NumberFormatHelper {

    val fiatAmountFormat: NumberFormat
        get() {
            val numberFormat = NumberFormat.getInstance()
            numberFormat.maximumFractionDigits = 2
            numberFormat.minimumFractionDigits = 2
            return numberFormat
        }

}
