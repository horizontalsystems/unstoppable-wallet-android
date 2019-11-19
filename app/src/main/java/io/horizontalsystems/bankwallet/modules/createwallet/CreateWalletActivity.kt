package io.horizontalsystems.bankwallet.modules.createwallet

import android.os.Bundle
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.components.CellView
import io.horizontalsystems.bankwallet.core.EosUnsupportedException
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.dialogs.AlertDialogFragment
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
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
                rightBtnItem = TopMenuItem(text = R.string.Button_Create, onClick = { presenter.didClickCreate() })
        )

        presenter = ViewModelProviders.of(this, CreateWalletModule.Factory()).get(CreateWalletPresenter::class.java)

        observeView(presenter.view as CreateWalletView)
        observeRouter(presenter.router as CreateWalletRouter)

        coinItemsAdapter = CoinItemsAdapter(presenter)
        coins.adapter = coinItemsAdapter

        presenter.viewDidLoad()
    }

    private fun observeView(view: CreateWalletView) {
        view.itemsLiveData.observe(this, Observer {
            coinItemsAdapter.items = it
            coinItemsAdapter.notifyDataSetChanged()
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
    }
}

class CoinItemsAdapter(private val presenter: CreateWalletPresenter) : RecyclerView.Adapter<SwitchableViewHolder>() {
    var items = listOf<CreateWalletModule.CoinViewItem>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwitchableViewHolder {
        val cellView = CellView(parent.context)
        cellView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return SwitchableViewHolder(cellView, presenter)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: SwitchableViewHolder, position: Int) {
        holder.bind(items[position], position == itemCount - 1)
    }
}

class SwitchableViewHolder(override val containerView: CellView, private val presenter: CreateWalletPresenter) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    init {
        containerView.setOnSingleClickListener {
            presenter.didTapItem(adapterPosition)
        }
    }

    fun bind(coinViewItem: CreateWalletModule.CoinViewItem, lastElement: Boolean) {
        containerView.coinIcon = coinViewItem.code
        containerView.title = coinViewItem.code
        containerView.subtitle = coinViewItem.title
        containerView.checked = coinViewItem.selected
        containerView.bottomBorder = lastElement
    }
}
