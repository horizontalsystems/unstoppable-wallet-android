package io.horizontalsystems.bankwallet.modules.blockchainsettingslist

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.BlockchainSetting
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType

object BlockchainSettingsListModule {

    interface IView {
        fun updatedViewItems(viewItems: List<BlockchainSettingListViewItem>)
    }

    interface IViewDelegate {
        fun onSelect(viewItem: BlockchainSettingListViewItem)
        fun onDone()
        fun onViewResume()
    }

    interface IInteractor {
        val coinsWithSettings: List<Coin>
        fun blockchainSettings(coinType: CoinType): BlockchainSetting?
    }

    interface IRouter {
        fun closeWithResultOk()
        fun close()
        fun openBlockchainSetting(type: CoinType)
    }

    fun startForResult(context: AppCompatActivity, coinTypes: List<CoinType>, showDoneButton: Boolean) {
        val intent = Intent(context, BlockchainSettingsListActivity::class.java)
        intent.putParcelableArrayListExtra(ModuleField.COIN_TYPES, ArrayList(coinTypes))
        intent.putExtra(ModuleField.SHOW_DONE_BUTTON, showDoneButton)
        context.startActivityForResult(intent, ModuleCode.BLOCKCHAIN_SETTINGS_LIST)
    }

    class Factory(private val coinTypes: List<CoinType>, private val showDoneButton: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = BlockchainSettingsListView()
            val router = BlockchainSettingsListRouter()
            val interactor = BlockchainSettingsListInteractor(App.blockchainSettingsManager)
            val presenter = BlockchainSettingsListPresenter(view, router, interactor, coinTypes, showDoneButton)

            return presenter as T
        }
    }
}
