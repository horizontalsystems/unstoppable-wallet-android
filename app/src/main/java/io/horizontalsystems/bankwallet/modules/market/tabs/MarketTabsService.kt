package io.horizontalsystems.bankwallet.modules.market.tabs

import io.horizontalsystems.bankwallet.core.IMarketStorage
import io.reactivex.subjects.PublishSubject

class MarketTabsService(private val storage: IMarketStorage) {

    val tabs = Tab.values()
    var currentTab: Tab
        get() = storage.currentTab ?: tabs.first()
        set(value) {
            storage.currentTab = value

            currentTabChangedObservable.onNext(Unit)
        }

    val currentTabChangedObservable = PublishSubject.create<Unit>()

    enum class Tab {
        Overview, Discovery, Favorites;

        companion object {
            private val map = values().associateBy(Tab::name)

            fun fromString(type: String?): Tab? = map[type]
        }
    }

}
