package io.horizontalsystems.bankwallet.modules.market

import io.horizontalsystems.bankwallet.core.IMarketStorage
import io.reactivex.subjects.PublishSubject

class MarketCategoriesService(private val storage: IMarketStorage) {

    val categories = Category.values()
    var currentCategory: Category
        get() = storage.currentCategory ?: categories.first()
        set(value) {
            storage.currentCategory = value

            currentCategoryChangedObservable.onNext(Unit)
        }

    val currentCategoryChangedObservable = PublishSubject.create<Unit>()

    enum class Category {
        Top100, DeFi, Favorites;

        companion object {
            private val map = values().associateBy(Category::name)

            fun fromString(type: String?): Category? = map[type]
        }
    }

}
