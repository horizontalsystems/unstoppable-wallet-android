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
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageKeysModule
import io.horizontalsystems.bankwallet.ui.dialogs.BottomManageKeysAlert
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_manage_coins.*
import kotlinx.android.synthetic.main.view_holder_coin_disabled.*
import kotlinx.android.synthetic.main.view_holder_coin_enabled.*


class ManageWalletsActivity : BaseActivity(), ManageWalletsAdapter.StartDragListener {

    private lateinit var viewModel: ManageWalletsViewModel
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_coins)

        viewModel = ViewModelProviders.of(this).get(ManageWalletsViewModel::class.java)
        viewModel.init()

        val adapter = ManageWalletsAdapter(viewModel.delegate, this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        itemTouchHelper = ItemTouchHelper(ManageWalletsDragHelper(adapter))
        itemTouchHelper?.attachToRecyclerView(recyclerView)

        shadowlessToolbar.bind(
                title = getString(R.string.ManageCoins_title),
                leftBtnItem = TopMenuItem(R.drawable.back) { onBackPressed() },
                rightBtnItem = TopMenuItem(R.drawable.checkmark_orange) { viewModel.delegate.saveChanges() }
        )

        viewModel.coinsLoadedLiveEvent.observe(this, Observer {
            adapter.notifyDataSetChanged()
        })

        viewModel.showNoAccountLiveEvent.observe(this, Observer { coin ->
            coin?.let {
                BottomManageKeysAlert.show(this, coin, object : BottomManageKeysAlert.Listener {
                    override fun onClickManageKeys() {
                        viewModel.delegate.onClickManageKeys()
                    }
                })
            }
        })

        viewModel.startManageKeysLiveEvent.observe(this, Observer {
            ManageKeysModule.start(this)
        })

        viewModel.closeLiveDate.observe(this, Observer {
            finish()
        })
    }

    override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper?.startDrag(viewHolder)
    }
}

class ManageWalletsAdapter(private val viewDelegate: ManageWalletsModule.IViewDelegate, private var startDragListener: StartDragListener)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ManageWalletsDragHelper.Listener {

    interface StartDragListener {
        fun requestDrag(viewHolder: RecyclerView.ViewHolder)
    }

    private val typeEnabled = 0
    private val typeDisabled = 1
    private val typeDivider = 2

    override fun getItemCount() = viewDelegate.enabledCoinsCount + viewDelegate.disabledCoinsCount + (if (showDivider) 1 else 0)

    override fun getItemViewType(position: Int): Int = when {
        position < viewDelegate.enabledCoinsCount -> typeEnabled
        showDivider && position == viewDelegate.enabledCoinsCount -> typeDivider
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
                holder.bind(coin = viewDelegate.enabledItemForIndex(position).coin, showBottomShadow = (position == viewDelegate.enabledCoinsCount - 1)) {
                    viewDelegate.disableCoin(position)
                }

                holder.dragIcon.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        startDragListener.requestDrag(holder)
                    }
                    false
                }
            }
            is ViewHolderDisabledCoin -> {
                holder.bind(coin = viewDelegate.disabledItemForIndex(disabledIndex(position)), showBottomShadow = (position == itemCount - 1)) {
                    viewDelegate.enableCoin(disabledIndex(position))
                }
            }
        }

    }

    // Drag Listener

    override fun onItemMoved(from: Int, to: Int) {
        notifyItemMoved(from, to)
    }

    override fun onItemMoveEnded(from: Int, to: Int) {
        viewDelegate.moveCoin(from, to)
    }

    private val showDivider
        get() = viewDelegate.enabledCoinsCount > 0

    private fun disabledIndex(position: Int): Int = when {
        showDivider -> position - viewDelegate.enabledCoinsCount - 1
        else -> position
    }
}

class ViewHolderEnabledCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(coin: Coin, showBottomShadow: Boolean, onClick: () -> (Unit)) {
        coinTitle.text = coin.title
        coinCode.text = coin.code
        coinIcon.bind(coin)
        enabledBottomShade.visibility = if (showBottomShadow) View.VISIBLE else View.GONE

        containerView.setOnClickListener { onClick.invoke() }
    }

}

class ViewHolderDisabledCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(coin: Coin, showBottomShadow: Boolean, onClick: () -> (Unit)) {
        disableCoinTitle.text = coin.title
        disableCoinCode.text = coin.code
        disableCoinIcon.bind(coin)
        disabledBottomShade.visibility = if (showBottomShadow) View.VISIBLE else View.GONE

        containerView.setOnClickListener { onClick.invoke() }
    }

}

class ViewHolderDivider(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
