package io.horizontalsystems.bankwallet.modules.market.discovery

import io.reactivex.Single

class MarketCategoryProvider {

    fun getCoinCodesByCategoryAsync(categoryId: String): Single<List<String>> {
        return Single.just(listOf("BTC", "ETH", "BCH", "CAKE"))
    }

    fun getCoinRatingsAsync(): Single<Map<String, String>> {
        return Single.just(mapOf("BTC" to "A", "ETH" to "B", "BCH" to "C", "CAKE" to "D"))
    }

}
