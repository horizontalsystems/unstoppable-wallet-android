package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IPriceAlertsStorage
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.reactivex.BackpressureStrategy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class PriceAlertManager(private val walletManager: IWalletManager, private val priceAlertsStorage: IPriceAlertsStorage) {

    private val priceAlertCountSubject = PublishSubject.create<Int>()

    val priceAlertCount: Int
        get() = priceAlertsStorage.priceAlertCount

    val priceAlertCountFlowable = priceAlertCountSubject.toFlowable(BackpressureStrategy.BUFFER)

    init {
        walletManager.walletsUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    val enabledCoins = walletManager.wallets.map { it.coin.code }

                    priceAlertsStorage.deleteExcluding(enabledCoins)
                    priceAlertCountSubject.onNext(priceAlertsStorage.priceAlertCount)
                }
    }

    fun getPriceAlerts(): List<PriceAlert> {
        val priceAlerts = priceAlertsStorage.all()

        return walletManager.wallets.map { wallet ->
            priceAlerts.firstOrNull { it.coin == wallet.coin } ?: PriceAlert(wallet.coin, PriceAlert.State.OFF)
        }
    }

    fun savePriceAlerts(priceAlerts: List<PriceAlert>) {
        priceAlertsStorage.save(priceAlerts.filter { it.state.value != null })
        priceAlertsStorage.delete(priceAlerts.filter { it.state.value == null })


        priceAlertCountSubject.onNext(priceAlertsStorage.priceAlertCount)
    }

}
