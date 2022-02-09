package io.horizontalsystems.bankwallet.modules.nft

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.Coin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

class NftsViewModel(private val service: NftsService) : ViewModel() {
    var priceType by mutableStateOf(PriceType.Days7)
        private set

    var viewState by mutableStateOf<ViewState?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    var collections by mutableStateOf<List<ViewItemNftCollection>>(listOf())
        private set

    init {
        loading = true

        viewModelScope.launch {
            delay(1000)

            val element = ViewItemNftCollection(
                slug = "devs-for-revolution",
                name = "CryptoPunks",
                imageUrl = "https://lh3.googleusercontent.com/wqIhMLnsbecLMdVsAShB0omdqgk4eSmVyNA914SsWOKWPZuhmqUvYHMh5bUWg4y48FZ9Wskr055BCFgA8zJofWzqpWaP57jwlB3K=s120",
                ownedAssetCount = 5,
                expanded = false,
                assets = listOf(
                    ViewItemNftAsset(
                        tokenId = "108510973921457929967077298367545831468135648058682555520544982493970263179265",
                        name = "Crypto Punk 312",
                        imageUrl = "https://lh3.googleusercontent.com/FalCKtVbAX1qBf2_O7g72UufouUsMStkpYfDAe3O-4OO06O4ESwcv63GAnKmEslOaaE4XUyy4X1xdc5CqDFtmDYVwXEFE5P9pUi_",
                        coinPrice = CoinValue(CoinValue.Kind.Coin(Coin("", "Ethereum", "ETH"), 8), BigDecimal("112.2979871")),
                        currencyPrice = CurrencyValue(Currency("USD", "$", 2), BigDecimal("112.2979871")),
                        onSale = true
                    ),
                    ViewItemNftAsset(
                        tokenId = "108510973921457929967077298367545831468135648058682555520544982493970263179265",
                        name = "Crypto Punk 312",
                        imageUrl = "https://lh3.googleusercontent.com/FalCKtVbAX1qBf2_O7g72UufouUsMStkpYfDAe3O-4OO06O4ESwcv63GAnKmEslOaaE4XUyy4X1xdc5CqDFtmDYVwXEFE5P9pUi_",
                        coinPrice = CoinValue(CoinValue.Kind.Coin(Coin("", "Ethereum", "ETH"), 8), BigDecimal("112.2979871")),
                        currencyPrice = CurrencyValue(Currency("USD", "$", 2), BigDecimal("112.2979871")),
                        onSale = false
                    ),
                    ViewItemNftAsset(
                        tokenId = "108510973921457929967077298367545831468135648058682555520544982493970263179265",
                        name = "Crypto Punk 312",
                        imageUrl = "https://lh3.googleusercontent.com/FalCKtVbAX1qBf2_O7g72UufouUsMStkpYfDAe3O-4OO06O4ESwcv63GAnKmEslOaaE4XUyy4X1xdc5CqDFtmDYVwXEFE5P9pUi_",
                        coinPrice = CoinValue(CoinValue.Kind.Coin(Coin("", "Ethereum", "ETH"), 8), BigDecimal("112.2979871")),
                        currencyPrice = CurrencyValue(Currency("USD", "$", 2), BigDecimal("112.2979871")),
                        onSale = true
                    )
                )
            )
            collections = listOf(
                element,
                element.copy(slug = "xxx")
            )

            viewState = ViewState.Success
            loading = false
        }

    }


    fun refresh() {
        TODO("Not yet implemented")
    }

    fun changePriceType(priceType: PriceType) {
        this.priceType = priceType
    }

    fun toggleCollection(collection: ViewItemNftCollection) {
        val index = collections.indexOf(collection)

        if (index != -1) {
            collections = collections.toMutableList().apply {
                this[index] = collection.copy(expanded = !collection.expanded)
            }
        }
    }

}

data class ViewItemNftCollection(
    val slug: String,
    val name: String,
    val imageUrl: String,
    val ownedAssetCount: Long,
    val expanded: Boolean,
    val assets: List<ViewItemNftAsset>
)

data class ViewItemNftAsset(
    val tokenId: String,
    val name: String,
    val imageUrl: String,
    val coinPrice: CoinValue,
    val currencyPrice: CurrencyValue,
    val onSale: Boolean
) {
}