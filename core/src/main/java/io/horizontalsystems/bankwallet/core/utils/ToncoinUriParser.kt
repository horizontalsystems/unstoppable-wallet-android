package io.horizontalsystems.bankwallet.core.utils

import android.net.Uri

object ToncoinUriParser {
    fun getAddress(text: String): String? {
        val uri = Uri.parse(text)
        if (uri.scheme != "ton") {
            return null
        }
        return uri.path?.replace("/", "")
    }
}