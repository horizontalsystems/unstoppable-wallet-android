package io.horizontalsystems.bankwallet.ui.helpers

object ImageWebHelper {

    fun getCoinCategoryImageUrl(id: String): String {
        return "https://markets.nyc3.digitaloceanspaces.com/category-icons/ios/$id@3x.png"
    }

    fun getCoinIconUrl(id: String): String {
        return "https://markets.nyc3.digitaloceanspaces.com/coin-icons/ios/$id@3x.png"
    }
}