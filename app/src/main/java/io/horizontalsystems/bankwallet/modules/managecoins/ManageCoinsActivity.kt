package io.horizontalsystems.bankwallet.modules.managecoins

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_manage_coins.*
import kotlinx.android.synthetic.main.view_holder_coin_disabled.*
import kotlinx.android.synthetic.main.view_holder_coin_enabled.*


class ManageCoinsActivity : BaseActivity(), ManageCoinsAdapter.Listener, StartDragListener {

    private lateinit var viewModel: ManageCoinsViewModel
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ManageCoinsViewModel::class.java)
        viewModel.init()

        setContentView(R.layout.activity_manage_coins)

        val adapter = ManageCoinsAdapter(this, this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter.viewModel = viewModel
        itemTouchHelper = ItemTouchHelper(MyDragHelperCallback(adapter))
        itemTouchHelper?.attachToRecyclerView(recyclerView)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.close)
        supportActionBar?.title = getString(R.string.ManageCoins_title)

        viewModel.coinsLoadedLiveEvent.observe(this, Observer {
            adapter.notifyDataSetChanged()
        })

        viewModel.closeLiveDate.observe(this, Observer {
            finish()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_manage_coins, menu)
        LayoutHelper.tintMenuIcons(menu, ContextCompat.getColor(this, R.color.yellow_crypto))
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        R.id.action_done -> {
            viewModel.delegate.saveChanges()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onEnabledItemClick(coin: Coin) {
        viewModel.delegate.disableCoin(coin)
    }

    override fun onDisabledItemClick(coin: Coin) {
        viewModel.delegate.enableCoin(coin)
    }

    override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper?.startDrag(viewHolder)
    }
}

class ManageCoinsAdapter(
        private var listener: Listener,
        private var startDragListener: StartDragListener)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(), MyDragHelperCallback.Listener {

    interface Listener {
        fun onEnabledItemClick(coin: Coin)
        fun onDisabledItemClick(coin: Coin)
    }

    lateinit var viewModel: ManageCoinsViewModel

    private val typeEnabled = 0
    private val typeDisabled = 1
    private val typeDivider = 2

    override fun getItemCount() = viewModel.delegate.enabledCoinsCount + viewModel.delegate.disabledCoinsCount + (if (showDivider) 1 else 0)

    override fun getItemViewType(position: Int): Int = when {
        position < viewModel.delegate.enabledCoinsCount -> typeEnabled
        showDivider && position == viewModel.delegate.enabledCoinsCount -> typeDivider
        else -> typeDisabled
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            typeEnabled -> ViewHolderEnabledCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_enabled, parent, false))
            typeDisabled -> ViewHolderDisabledCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_disabled, parent, false))
            else -> ViewHolderDivider(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_manage_coins_divider, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderEnabledCoin -> {
                val transactionRecord = viewModel.delegate.enabledItemForIndex(position)
                holder.bind(transactionRecord) { listener.onEnabledItemClick(transactionRecord) }

                holder.dragIcon.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        startDragListener.requestDrag(holder)
                    }
                    false
                }
            }
            is ViewHolderDisabledCoin -> {
                val transactionRecord = viewModel.delegate.disabledItemForIndex(disabledIndex(position))
                holder.bind(transactionRecord) { listener.onDisabledItemClick(transactionRecord) }
            }
        }

    }

    override fun onItemMoved(from: Int, to: Int) {
        notifyItemMoved(from, to)
    }

    private val showDivider
        get() = viewModel.delegate.enabledCoinsCount > 0

    private fun disabledIndex(position: Int): Int = when {
        showDivider -> position - viewModel.delegate.enabledCoinsCount - 1
        else -> position
    }
}

class ViewHolderEnabledCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(coin: Coin, onClick: () -> (Unit)) {
        coinTitle.text = coin.title
        coinCode.text = coin.code

        val iconDrawable = ContextCompat.getDrawable(containerView.context, LayoutHelper.getCoinDrawableResource(TextHelper.getCleanCoinCode(coin.code)))
        coinIcon.setImageDrawable(iconDrawable)
        minusIcon.setOnSingleClickListener { onClick.invoke() }
    }

}

class ViewHolderDisabledCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(coin: Coin, onClick: () -> (Unit)) {
        disableCoinTitle.text = coin.title
        disableCoinCode.text = coin.code

        val iconDrawable = ContextCompat.getDrawable(containerView.context, LayoutHelper.getCoinDrawableResource(TextHelper.getCleanCoinCode(coin.code)))
        disableCoinIcon.setImageDrawable(iconDrawable)
        plusIcon.setOnSingleClickListener { onClick.invoke() }
    }

}

class ViewHolderDivider(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer


class MyDragHelperCallback(private var listener: Listener) : ItemTouchHelper.Callback() {

    interface Listener {
        fun onItemMoved(from: Int, to: Int)
    }

    private val drawMovementFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return makeMovementFlags(drawMovementFlags, 0)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        listener.onItemMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(recyclerView: RecyclerView.ViewHolder, position: Int) { }

}

interface StartDragListener {
    fun requestDrag(viewHolder: RecyclerView.ViewHolder)
}
