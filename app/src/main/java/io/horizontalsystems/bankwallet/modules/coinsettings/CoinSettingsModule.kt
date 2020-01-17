package io.horizontalsystems.bankwallet.modules.coinsettings

import android.content.Intent
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.SyncMode
import kotlinx.android.parcel.Parcelize

object CoinSettingsModule {

    interface IView {
        fun setSelection(derivation: Derivation, syncMode: SyncMode)
    }

    interface IViewDelegate {
        fun onLoad()
        fun onSelect(syncMode: SyncMode)
        fun onSelect(derivation: Derivation)
        fun onDone()
    }

    interface IInteractor {
        fun bitcoinDerivation(): Derivation
        fun syncMode(): SyncMode
        fun updateBitcoinDerivation(derivation: Derivation)
        fun updateSyncMode(source: SyncMode)
    }

    interface IRouter {
        fun closeWithResultOk()
        fun close()
    }

    fun startForResult(context: AppCompatActivity, mode: SettingsMode = SettingsMode.Settings) {
        val intent = Intent(context, CoinSettingsActivity::class.java)
        intent.putParcelableExtra(ModuleField.COIN_SETTINGS_MODE, mode)
        context.startActivityForResult(intent, ModuleCode.COIN_SETTINGS)
    }

    fun start(context: AppCompatActivity, mode: SettingsMode = SettingsMode.Settings) {
        val intent = Intent(context, CoinSettingsActivity::class.java)
        intent.putParcelableExtra(ModuleField.COIN_SETTINGS_MODE, mode)
        context.startActivity(intent)
    }

    class Factory(private val mode: SettingsMode) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = CoinSettingsView()
            val router = CoinSettingsRouter()
            val interactor = CoinSettingsInteractor(App.coinSettingsManager)
            val presenter = CoinSettingsPresenter(mode, view, router, interactor)

            return presenter as T
        }
    }
}

@Parcelize
enum class SettingsMode: Parcelable {
    Settings,
    Restore
}
