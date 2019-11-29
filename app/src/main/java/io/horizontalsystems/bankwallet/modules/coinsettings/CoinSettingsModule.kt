package io.horizontalsystems.bankwallet.modules.coinsettings

import android.content.Intent
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.bankwallet.entities.SyncMode
import kotlinx.android.parcel.Parcelize

object CoinSettingsModule {

    interface IView {
        fun update(derivation: Derivation)
        fun setTitle(title: String)
        fun update(syncMode: SyncMode, coinTitle: String)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onSelect(syncMode: SyncMode)
        fun onSelect(derivation: Derivation)
        fun onDone()
        fun onCancel()
    }

    interface IRouter {
        fun notifyOptions(coinSettings: CoinSettings, coin: Coin)
        fun onCancelClick()
    }

    fun startForResult(coin: Coin, coinSettings: CoinSettings, context: AppCompatActivity) {
        val intent = Intent(context, CoinSettingsActivity::class.java)
        intent.putParcelableExtra(ModuleField.COIN, coin)
        intent.putParcelableExtra(ModuleField.COIN_SETTINGS, CoinSettingsWrapped(coinSettings))
        context.startActivityForResult(intent, ModuleCode.COIN_SETTINGS)
    }

    class Factory(private val coin: Coin, private val coinSettings: CoinSettings) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = CoinSettingsView()
            val router = CoinSettingsRouter()
            val presenter = CoinSettingsPresenter(coin, coinSettings, view, router)

            return presenter as T
        }
    }
}

@Parcelize
class CoinSettingsWrapped(var settings: CoinSettings) : Parcelable
