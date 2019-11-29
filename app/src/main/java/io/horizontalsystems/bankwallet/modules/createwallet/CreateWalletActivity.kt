package io.horizontalsystems.bankwallet.modules.createwallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.components.CellView
import io.horizontalsystems.bankwallet.core.EosUnsupportedException
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.PresentationMode
import io.horizontalsystems.bankwallet.modules.coinsettings.CoinSettingsModule
import io.horizontalsystems.bankwallet.modules.coinsettings.CoinSettingsWrapped
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletViewItem
import io.horizontalsystems.bankwallet.ui.dialogs.AlertDialogFragment
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_create_wallet.*

class CreateWalletActivity : BaseActivity() {
    private lateinit var presenter: CreateWalletPresenter
    private lateinit var coinItemsAdapter: CoinItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_wallet)
        shadowlessToolbar.bind(
                title = getString(R.string.Create_Title),
                leftBtnItem = TopMenuItem(text = R.string.Button_Cancel, onClick = { onBackPressed() }),
                rightBtnItem = TopMenuItem(text = R.string.Button_Create, onClick = { presenter.onCreateButtonClick() })
        )

        val presentationMode: PresentationMode = intent.getParcelableExtra(ModuleField.PRESENTATION_MODE)
                ?: PresentationMode.Initial
        val predefinedAccountTypeString = intent.extras?.getString(ModuleField.PREDEFINED_ACCOUNT_TYPE)
        val predefinedAccountType = PredefinedAccountType.fromString(predefinedAccountTypeString)

        presenter = ViewModelProvider(this, CreateWalletModule.Factory(presentationMode, predefinedAccountType)).get(CreateWalletPresenter::class.java)

        observeView(presenter.view as CreateWalletView)
        observeRouter(presenter.router as CreateWalletRouter)

        coinItemsAdapter = CoinItemsAdapter(presenter)
        coins.adapter = coinItemsAdapter

        presenter.onLoad()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ModuleCode.COIN_SETTINGS -> {
                if (resultCode == Activity.RESULT_CANCELED) {
                    presenter.onCancelSelectingCoinSettings()
                } else if (resultCode == Activity.RESULT_OK && data != null) {
                    val coin = data.getParcelableExtra<Coin>(ModuleField.COIN) ?: return
                    val coinSettings = data.getParcelableExtra<CoinSettingsWrapped>(ModuleField.COIN_SETTINGS)
                            ?: return

                    presenter.onSelectCoinSettings(coinSettings.settings, coin)
                }
            }
        }
    }

    private fun observeView(view: CreateWalletView) {
        view.coinsLiveData.observe(this, Observer { (featured, coins) ->
            coinItemsAdapter.featuredCoins = featured
            coinItemsAdapter.coins = coins
            coinItemsAdapter.notifyDataSetChanged()
        })

        view.createButtonEnabled.observe(this, Observer { enabled ->
            shadowlessToolbar?.setRightTextButtonState(enabled)
        })

        view.errorLiveEvent.observe(this, Observer {
            if (it is EosUnsupportedException) {
                AlertDialogFragment.newInstance(
                        R.string.Alert_TitleWarning,
                        R.string.ManageCoins_EOSAlert_CreateButton,
                        R.string.Alert_Ok
                ).show(supportFragmentManager, "alert_dialog")
            }
        })
    }

    private fun observeRouter(router: CreateWalletRouter) {
        router.startMainModuleLiveEvent.observe(this, Observer {
            MainModule.startAsNewTask(this)
            finish()
        })
        router.showCoinSettings.observe(this, Observer { (coin, coinSettings) ->
            CoinSettingsModule.startForResult(coin, coinSettings, this)
        })
        router.close.observe(this, Observer {
            finish()
        })
    }
}

class CoinItemsAdapter(private val presenter: CreateWalletPresenter) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val typeFeatured = 0
    private val typeAll = 1
    private val typeDivider = 2

    private val showDivider
        get() = featuredCoins.isNotEmpty()

    var featuredCoins = listOf<ManageWalletViewItem>()
    var coins = listOf<ManageWalletViewItem>()

    override fun getItemViewType(position: Int): Int = when {
        position < featuredCoins.size -> typeFeatured
        showDivider && position == featuredCoins.size -> typeDivider
        else -> typeAll
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            typeFeatured, typeAll -> {
                val cellView = CellView(parent.context)
                cellView.layoutParams = getCellViewLayoutParams()
                CoinViewHolder(cellView) { isChecked, index ->
                    onSwitchChanged(isChecked, index)
                }
            }
            else -> ViewHolderDivider(inflate(parent, R.layout.view_holder_coin_manager_divider, false))
        }
    }

    private fun onSwitchChanged(isChecked: Boolean, index: Int) {
        if (isChecked) {
            presenter.onEnable(getItemByPosition(index))
        } else {
            presenter.onDisable(getItemByPosition(index))
        }
    }

    private fun getCellViewLayoutParams() =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override fun getItemCount(): Int {
        return featuredCoins.size + coins.size + (if (showDivider) 1 else 0)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CoinViewHolder -> {
                val item = getItemByPosition(position)
                holder.bind(item, isLastItemInGroup(position))
            }
        }
    }

    private fun isLastItemInGroup(position: Int): Boolean {
        return if (position == featuredCoins.size - 1) {
            true
        } else {
            val dividerCount = if (showDivider) 1 else 0
            position == itemCount - dividerCount
        }
    }

    private fun getItemByPosition(position: Int): ManageWalletViewItem {
        return if (position < featuredCoins.size) {
            featuredCoins[position]
        } else {
            val index = when {
                showDivider -> position - featuredCoins.size - 1
                else -> position
            }
            coins[index]
        }
    }
}

class CoinViewHolder(override val containerView: CellView, onClick: (isChecked: Boolean, position: Int) -> Unit) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    init {
        containerView.switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            onClick.invoke(isChecked, adapterPosition)
        }
    }

    fun bind(coinViewItem: ManageWalletViewItem, lastElement: Boolean) {
        containerView.coinIcon = coinViewItem.coin.code
        containerView.title = coinViewItem.coin.code
        containerView.subtitle = coinViewItem.coin.title
        containerView.subtitleLabel = coinViewItem.coin.type.typeLabel()
        containerView.switchIsChecked = coinViewItem.enabled
        containerView.bottomBorder = lastElement
    }
}

class ViewHolderDivider(val containerView: View) : RecyclerView.ViewHolder(containerView)
