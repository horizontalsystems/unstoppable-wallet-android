package io.horizontalsystems.bankwallet.modules.managecoins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
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

        setTransparentStatusBar()

        setContentView(R.layout.activity_manage_coins)

        val adapter = ManageCoinsAdapter(this, this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter.viewModel = viewModel
        itemTouchHelper = ItemTouchHelper(MyDragHelperCallback(adapter))
        itemTouchHelper?.attachToRecyclerView(recyclerView)

        shadowlessToolbar.bind(
                title = getString(R.string.ManageCoins_title),
                leftBtnItem = TopMenuItem(R.drawable.back, { onBackPressed() }),
                rightBtnItem = TopMenuItem(R.drawable.checkmark_orange, { viewModel.delegate.saveChanges() })
        )

        viewModel.coinsLoadedLiveEvent.observe(this, Observer {
            adapter.notifyDataSetChanged()
        })

        viewModel.closeLiveDate.observe(this, Observer {
            finish()
        })
    }

    override fun onEnabledItemClick(position: Int) {
        viewModel.delegate.disableCoin(position)
    }

    override fun onDisabledItemClick(position: Int) {
        viewModel.delegate.enableCoin(position)
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
        fun onEnabledItemClick(position: Int)
        fun onDisabledItemClick(position: Int)
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
                holder.bind(
                        coin = transactionRecord,
                        showBottomShadow = (position == viewModel.delegate.enabledCoinsCount-1),
                        onClick = { listener.onEnabledItemClick(position) }
                )

                holder.dragIcon.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        startDragListener.requestDrag(holder)
                    }
                    false
                }
            }
            is ViewHolderDisabledCoin -> {
                val transactionRecord = viewModel.delegate.disabledItemForIndex(disabledIndex(position))
                holder.bind(
                        coin = transactionRecord,
                        showBottomShadow = (position == itemCount - 1),
                        onClick = { listener.onDisabledItemClick(disabledIndex(position)) }
                )
            }
        }

    }

    override fun onItemMoved(from: Int, to: Int) {
        notifyItemMoved(from, to)
    }

    override fun onItemMoveEnded(from: Int, to: Int) {
        viewModel.delegate.moveCoin(from, to)
    }

    private val showDivider
        get() = viewModel.delegate.enabledCoinsCount > 0

    private fun disabledIndex(position: Int): Int = when {
        showDivider -> position - viewModel.delegate.enabledCoinsCount - 1
        else -> position
    }
}

class ViewHolderEnabledCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(coin: Coin, showBottomShadow: Boolean, onClick: () -> (Unit)) {
        coinTitle.text = coin.title
        coinCode.text = coin.code
        coinIcon.bind(coin)
        enabledBottomShade.visibility = if (showBottomShadow) View.VISIBLE else View.GONE

        minusIcon.setOnSingleClickListener { onClick.invoke() }
    }

}

class ViewHolderDisabledCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(coin: Coin, showBottomShadow: Boolean, onClick: () -> (Unit)) {
        disableCoinTitle.text = coin.title
        disableCoinCode.text = coin.code
        disableCoinIcon.bind(coin)
        disabledBottomShade.visibility = if (showBottomShadow) View.VISIBLE else View.GONE

        plusIcon.setOnSingleClickListener { onClick.invoke() }
    }

}

class ViewHolderDivider(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer


class MyDragHelperCallback(private var listener: Listener) : ItemTouchHelper.Callback() {

    var dragFrom = -1
    var dragTo = -1

    interface Listener {
        fun onItemMoved(from: Int, to: Int)
        fun onItemMoveEnded(from: Int, to: Int)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    private val drawMovementFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return makeMovementFlags(drawMovementFlags, 0)
    }

    override fun canDropOver(recyclerView: RecyclerView, current: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return current.itemViewType == target.itemViewType
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition
        if(dragFrom == -1) {
            dragFrom = fromPosition
        }
        dragTo = toPosition

        listener.onItemMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(recyclerView: RecyclerView.ViewHolder, position: Int) { }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if(dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
            listener.onItemMoveEnded(dragFrom, dragTo)
        }

        dragFrom = -1
        dragTo = -1
    }
}

interface StartDragListener {
    fun requestDrag(viewHolder: RecyclerView.ViewHolder)
}
