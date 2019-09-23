package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IPriceAlertsStorage
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.PublishSubject

class PriceAlertManager(private val walletManager: IWalletManager, private val priceAlertsStorage: IPriceAlertsStorage) {

    private val priceAlertCountSubject = PublishSubject.create<Int>()

    val priceAlertCount: Int
        get() = priceAlertsStorage.priceAlertCount

    val priceAlertCountFlowable = priceAlertCountSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun getPriceAlerts(): List<PriceAlert> {
        val priceAlerts = priceAlertsStorage.all()

        return walletManager.wallets.map { wallet ->
            priceAlerts.firstOrNull { it.coin == wallet.coin } ?: PriceAlert(wallet.coin, PriceAlert.State.OFF)
        }
    }

    fun savePriceAlert(priceAlert: PriceAlert) {
        if (priceAlert.state.value != null) {
            priceAlertsStorage.save(priceAlert)
        } else {
            priceAlertsStorage.delete(priceAlert)
        }

        priceAlertCountSubject.onNext(priceAlertsStorage.priceAlertCount)
    }

}
