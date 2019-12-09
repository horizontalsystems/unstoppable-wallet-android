package io.horizontalsystems.bankwallet.modules.coinsettings

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.SyncMode
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_coin_settings.*
import kotlinx.android.synthetic.main.view_holder_coin_setting_item.*
import kotlinx.android.synthetic.main.view_holder_coin_setting_section_description.*
import kotlinx.android.synthetic.main.view_holder_coin_setting_section_header.*


class CoinSettingsActivity : BaseActivity(), CoinSettingsAdapter.Listener {

    private lateinit var presenter: CoinSettingsPresenter
    private lateinit var adapter: CoinSettingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coin_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val coin = intent.getParcelableExtra<Coin>(ModuleField.COIN)
        val coinSettings = intent.getParcelableExtra<CoinSettingsWrapped>(ModuleField.COIN_SETTINGS)
        val settingsMode = intent.getParcelableExtra<SettingsMode>(ModuleField.COIN_SETTINGS_MODE)

        presenter = ViewModelProvider(this, CoinSettingsModule.Factory(coin, coinSettings.settings, settingsMode))
                .get(CoinSettingsPresenter::class.java)

        presenter.viewDidLoad()

        adapter = CoinSettingsAdapter(this)
        recyclerView.adapter = adapter

        //disable default item animation(blinking)
        val animator = recyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        observeView(presenter.view as CoinSettingsView)
        observeRouter(presenter.router as CoinSettingsRouter)
    }

    private fun observeView(view: CoinSettingsView) {
        view.titleData.observe(this, Observer { title ->
            collapsingToolbar.title = title
        })

        view.viewItems.observe(this, Observer { items ->
            adapter.items = items
            adapter.notifyDataSetChanged()
        })
    }

    private fun observeRouter(router: CoinSettingsRouter) {
        router.notifyOptionsLiveEvent.observe(this, Observer { (coin, coinSettings) ->
            setResult(RESULT_OK, Intent().apply {
                putParcelableExtra(ModuleField.COIN, coin)
                putParcelableExtra(ModuleField.COIN_SETTINGS, CoinSettingsWrapped(coinSettings))
            })

            finish()
        })

        router.onCancelClick.observe(this, Observer {
            setResult(RESULT_CANCELED, Intent())
            finish()
        })
    }

    override fun onBackPressed() {
        presenter.onCancel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.coin_settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menuEnable -> {
                presenter.onDone()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDerivationSelect(derivation: Derivation) {
        presenter.onSelect(derivation)
    }

    override fun onSyncModeSelect(syncMode: SyncMode) {
        presenter.onSelect(syncMode)
    }
}

class CoinSettingsAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val viewTypeDerivation = 1
    private val viewTypeSyncMode = 2
    private val viewTypeSectionHeader = 3
    private val viewTypeSectionDescription = 4

    interface Listener {
        fun onDerivationSelect(derivation: Derivation)
        fun onSyncModeSelect(syncMode: SyncMode)
    }

    var items = listOf<SettingSection>()

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int =
            when (items[position]) {
                is SettingSection.DerivationItem -> viewTypeDerivation
                is SettingSection.SyncModeItem -> viewTypeSyncMode
                is SettingSection.Header -> viewTypeSectionHeader
                is SettingSection.Description -> viewTypeSectionDescription
            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            viewTypeDerivation -> ViewHolderDerivation(inflater.inflate(
                    R.layout.view_holder_coin_setting_item, parent, false),
                    onDerivationClick = { index ->
                        (items[index] as? SettingSection.DerivationItem)?.derivation?.let {
                            updateItems(it)
                            listener.onDerivationSelect(it)
                        }
                    })
            viewTypeSyncMode -> ViewHolderSyncMode(inflater.inflate(
                    R.layout.view_holder_coin_setting_item, parent, false),
                    onSyncModeClick = { index ->
                        (items[index] as? SettingSection.SyncModeItem)?.syncMode?.let {
                            updateItems(it)
                            listener.onSyncModeSelect(it)
                        }
                    })
            viewTypeSectionHeader -> ViewHolderCoinSettingSectionHeader(inflater.inflate(
                    R.layout.view_holder_coin_setting_section_header, parent, false))
            viewTypeSectionDescription -> ViewHolderCoinSettingSectionDescription(inflater.inflate(
                    R.layout.view_holder_coin_setting_section_description, parent, false))
            else -> throw Exception("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderDerivation -> {
                val item = (items[position] as? SettingSection.DerivationItem) ?: return
                holder.bind(item)
            }
            is ViewHolderSyncMode -> {
                val item = (items[position] as? SettingSection.SyncModeItem) ?: return
                holder.bind(item)
            }
            is ViewHolderCoinSettingSectionHeader -> {
                val item = (items[position] as? SettingSection.Header) ?: return
                holder.bind(item.text)
            }
            is ViewHolderCoinSettingSectionDescription -> {
                val item = (items[position] as? SettingSection.Description) ?: return
                holder.bind(item.text)
            }
        }
    }

    private fun updateItems(derivation: Derivation) {
        items.forEachIndexed { index, item ->
            if (item is SettingSection.DerivationItem) {
                item.selected = derivation == item.derivation
                notifyItemChanged(index)
            }
        }
    }

    private fun updateItems(syncMode: SyncMode) {
        items.forEachIndexed { index, item ->
            if (item is SettingSection.SyncModeItem) {
                item.selected = syncMode == item.syncMode
                notifyItemChanged(index)
            }
        }
    }
}

class ViewHolderDerivation(override val containerView: View, private val onDerivationClick: (position: Int) -> Unit)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener {
            onDerivationClick.invoke(adapterPosition)
        }
    }

    fun bind(item: SettingSection.DerivationItem) {
        itemTitle.setText(item.title)
        itemSubtitle.setText(item.subtitle)
        checkMark.visibility = if (item.selected) View.VISIBLE else View.INVISIBLE
    }
}

class ViewHolderSyncMode(override val containerView: View, private val onSyncModeClick: (position: Int) -> Unit)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener {
            onSyncModeClick.invoke(adapterPosition)
        }
    }

    fun bind(item: SettingSection.SyncModeItem) {
        itemTitle.text = item.title
        itemSubtitle.setText(item.subtitle)
        checkMark.visibility = if (item.selected) View.VISIBLE else View.INVISIBLE
    }
}

class ViewHolderCoinSettingSectionHeader(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(header: String) {
        sectionHeader.text = header
    }
}

class ViewHolderCoinSettingSectionDescription(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(description: String) {
        descriptionText.text = description
    }
}
