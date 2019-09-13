package io.horizontalsystems.bankwallet.modules.createwallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.main.MainModule
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_create_wallet.*
import kotlinx.android.synthetic.main.view_holder_switchable.*

class CreateWalletActivity : BaseActivity() {
    private lateinit var presenter: CreateWalletPresenter
    private lateinit var coinItemsAdapter: CoinItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_wallet)

        presenter = ViewModelProviders.of(this, CreateWalletModule.Factory()).get(CreateWalletPresenter::class.java)

        observeView(presenter.view as CreateWalletView)
        observeRouter(presenter.router as CreateWalletRouter)

        buttonCreate.setOnSingleClickListener {
            presenter.didCreate()
        }

        coinItemsAdapter = CoinItemsAdapter(presenter)
        coins.adapter = coinItemsAdapter

        presenter.viewDidLoad()
    }

    private fun observeView(view: CreateWalletView) {
        view.itemsLiveData.observe(this, Observer {
            coinItemsAdapter.items = it
        })

        view.createEnabledLiveData.observe(this, Observer {
            buttonCreate.isEnabled = it
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwitchableViewHolder {
        val containerView = LayoutInflater.from(parent.context).inflate(SwitchableViewHolder.layoutResourceId, parent, false)
        return SwitchableViewHolder(containerView, presenter)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: SwitchableViewHolder, position: Int) {
        holder.bind(items[position])
    }
}

class SwitchableViewHolder(override val containerView: View, private val presenter: CreateWalletPresenter) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    init {
        enabled.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                presenter.didEnable(adapterPosition)
            } else {
                presenter.didDisable(adapterPosition)
            }
        }
    }

    fun bind(coinViewItem: CreateWalletModule.CoinViewItem) {
        title.text = coinViewItem.title
        enabled.isChecked = coinViewItem.selected
    }

    companion object {
        const val layoutResourceId = R.layout.view_holder_switchable
    }
}
