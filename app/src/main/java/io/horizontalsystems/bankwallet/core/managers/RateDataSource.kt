package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.Coin

class RateDataSource: IRateStorage {

    override fun rate(coin: Coin, currencyCode: String): Rate? {
        return when(coin) {
            "ETHt" -> Rate("ETHt", "USD", 200.0, 1542080725000)
            "BTCt" -> Rate("BTCt", "USD", 10_000.0, 1542080725000)
            else -> Rate("BTC", "USD", 6300.0, 1542080725000)
        }
    }

    override fun save(value: Double, coin: Coin, currencyCode: String) {
        Log.e("RateDataStorage", "save() called with value $value for currency $currencyCode")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clear() {
        Log.e("RateDataStorage", "clear() called")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
