package io.horizontalsystems.bankwallet.modules.market.discovery

import android.content.Context
import com.google.gson.Gson
import io.reactivex.Single
import java.io.InputStreamReader

class MarketCategoryProvider(private val context: Context) {

    private val coinCategoriesFileName = "coin_categories.json"

    private val coinCategories: CoinCategories by lazy {
        val inputStream = context.assets.open(coinCategoriesFileName)
        Gson().fromJson(InputStreamReader(inputStream), CoinCategories::class.java)
    }

    fun getCoinCodesByCategoryAsync(categoryId: String): Single<List<String>> =
            Single.create { emitter ->
                try {
                    val coins = coinCategories.coins.filter { it.categories.contains(categoryId) }
                    emitter.onSuccess(coins.map { it.code })
                } catch (error: Throwable) {
                    emitter.onError(error)
                }
            }

    fun getCoinRatingsAsync(): Single<Map<String, String>> =
            Single.create { emitter ->
                try {
                    val coinRatingsMap = mutableMapOf<String, String>()
                    coinCategories.coins.forEach { coin ->
                        if (coin.rating.isNotEmpty()) {
                            coinRatingsMap[coin.code] = coin.rating
                        }
                    }
                    emitter.onSuccess(coinRatingsMap)
                } catch (error: Throwable) {
                    emitter.onError(error)
                }
            }

    private data class CoinCategories(val categories: List<Category>, val coins: List<CategoryCoin>)

    private data class Category(val id: String, val name: String)

    private data class CategoryCoin(
            val code: String,
            val name: String,
            val categories: List<String>,
            val active: Boolean,
            val description: String,
            val details: Map<String, String>,
            val rating: String
    )

}
