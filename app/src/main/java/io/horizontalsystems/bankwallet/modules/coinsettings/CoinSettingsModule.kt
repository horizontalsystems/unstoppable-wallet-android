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
        fun setTitle(title: String)
        fun setItems(items: List<SettingSection>)
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

    fun startForResult(coin: Coin, coinSettings: CoinSettings, restoreMode: SettingsMode, context: AppCompatActivity) {
        val intent = Intent(context, CoinSettingsActivity::class.java)
        intent.putParcelableExtra(ModuleField.COIN, coin)
        intent.putParcelableExtra(ModuleField.COIN_SETTINGS, CoinSettingsWrapped(coinSettings))
        intent.putParcelableExtra(ModuleField.COIN_SETTINGS_MODE, restoreMode)
        context.startActivityForResult(intent, ModuleCode.COIN_SETTINGS)
    }

    class Factory(private val coin: Coin, private val coinSettings: CoinSettings, private val settingsMode: SettingsMode) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = CoinSettingsView()
            val router = CoinSettingsRouter()
            val presenter = CoinSettingsPresenter(coin, coinSettings, settingsMode, view, router)

            return presenter as T
        }
    }
}

@Parcelize
class CoinSettingsWrapped(var settings: CoinSettings) : Parcelable

sealed class SettingSection{
    class Header(val text: String): SettingSection()
    class Description(val text: String): SettingSection()
    class DerivationItem(val title: Int, val subtitle: Int, val derivation: Derivation, var selected: Boolean): SettingSection()
    class SyncModeItem(val title: String, val subtitle: Int, val syncMode: SyncMode, var selected: Boolean): SettingSection()
}

@Parcelize
enum class SettingsMode: Parcelable{
    Create,
    Restore,
    Manage
}
