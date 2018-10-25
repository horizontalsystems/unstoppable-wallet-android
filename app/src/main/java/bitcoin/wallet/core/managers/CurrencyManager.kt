package bitcoin.wallet.core.managers

import android.content.SharedPreferences
import android.text.TextUtils
import bitcoin.wallet.core.App
import bitcoin.wallet.core.ICurrencyManager
import bitcoin.wallet.entities.Currency
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable


class CurrencyManager : ICurrencyManager {

    override fun getBaseCurrencyFlowable(): Flowable<Currency> =
            Flowable.create({ emitter ->
                val emitSavedBaseCurrency = {
                    val currency = App.localStorage.baseCurrency
                    emitter.onNext(currency)
                }

                val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, updatedKey ->
                    if (TextUtils.equals(LocalStorageManager.BASE_CURRENCY, updatedKey)) {
                        emitSavedBaseCurrency()
                    }
                }

                App.preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
                emitter.setCancellable { App.preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener) }
            }, BackpressureStrategy.LATEST)
}
