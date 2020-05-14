package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IPriceAlertsStorage
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.reactivex.schedulers.Schedulers

class PriceAlertManager(private val walletManager: IWalletManager, private val priceAlertsStorage: IPriceAlertsStorage) {

    fun onAppLaunch(){
        val disposable = walletManager.walletsUpdatedObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { wallets ->
                    val enabledCoins = wallets.map { it.coin.code }

                    priceAlertsStorage.deleteExcluding(enabledCoins)
                }
    }

    fun getPriceAlerts(): List<PriceAlert> {
        val priceAlerts = priceAlertsStorage.all()

        return walletManager.wallets.map { wallet ->
            priceAlerts.firstOrNull { it.coin == wallet.coin }
                    ?: PriceAlert(wallet.coin, PriceAlert.State.OFF)
        }
    }

    fun savePriceAlerts(priceAlerts: List<PriceAlert>) {
        priceAlertsStorage.save(priceAlerts.filter { it.state.value != null })
        priceAlertsStorage.delete(priceAlerts.filter { it.state.value == null })
    }

}
