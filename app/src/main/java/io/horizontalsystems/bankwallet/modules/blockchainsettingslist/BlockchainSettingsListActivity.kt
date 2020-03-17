package io.horizontalsystems.bankwallet.modules.blockchainsettingslist

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsModule
import kotlinx.android.synthetic.main.activity_blockchain_settings_list.*
import kotlinx.android.synthetic.main.activity_coin_settings.toolbar

class BlockchainSettingsListActivity : BaseActivity(), BlockchainSettingsListAdapter.Listener {

    private lateinit var presenter: BlockchainSettingsListPresenter
    private lateinit var adapter: BlockchainSettingsListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blockchain_settings_list)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val coinTypes: List<CoinType> = intent.getParcelableArrayListExtra(ModuleField.COIN_TYPES) ?: listOf()
        val showDoneButton: Boolean = intent.getBooleanExtra(ModuleField.SHOW_DONE_BUTTON, false)

        presenter = ViewModelProvider(this, BlockchainSettingsListModule.Factory(coinTypes, showDoneButton))
                .get(BlockchainSettingsListPresenter::class.java)

        adapter = BlockchainSettingsListAdapter(this)
        recyclerView.adapter = adapter

        observeView(presenter.view as BlockchainSettingsListView)
        observeRouter(presenter.router as BlockchainSettingsListRouter)
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResume()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.coin_settings_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menuDone)?.apply {
            isVisible = presenter.showDoneButton
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuDone -> {
                presenter.onDone()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun select(viewItem: BlockchainSettingListViewItem) {
        presenter.onSelect(viewItem)
    }

    private fun observeView(view: BlockchainSettingsListView) {
        view.updateViewItems.observe(this, Observer {
            adapter.viewItems = it
            adapter.notifyDataSetChanged()
        })
    }

    private fun observeRouter(router: BlockchainSettingsListRouter) {
        router.closeWithResultOk.observe(this, Observer {
            setResult(RESULT_OK)
            finish()
        })

        router.openBlockchainSettings.observe(this, Observer { coinType ->
            BlockchainSettingsModule.startForResult(this, coinType)
        })

        router.close.observe(this, Observer {
            finish()
        })
    }

}
