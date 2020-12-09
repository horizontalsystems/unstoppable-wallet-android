package io.horizontalsystems.bankwallet.modules.market

class MarketCategoriesService() {

    val categories = Category.values()

    enum class Category {
        All, Favorites
    }

}
