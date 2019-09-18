package io.horizontalsystems.bankwallet.modules.balance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.appbar.AppBarLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.bankwallet.ui.dialogs.BackupAlertDialog
import io.horizontalsystems.bankwallet.ui.dialogs.BalanceSortDialogFragment
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_balance.*

class BalanceFragment : Fragment(), BalanceCoinAdapter.Listener, BalanceSortDialogFragment.Listener {

    private lateinit var viewModel: BalanceViewModel
    private lateinit var coinAdapter: BalanceCoinAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(BalanceViewModel::class.java)
        viewModel.init()
        coinAdapter = BalanceCoinAdapter(this, viewModel.delegate)

        recyclerCoins.adapter = coinAdapter
        recyclerCoins.layoutManager = NpaLinearLayoutManager(context)
        (recyclerCoins.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        sortButton.isActivated = true
        sortButton.setOnClickListener {
            viewModel.delegate.onSortClick()
        }

        chartButton.setOnClickListener {
            viewModel.delegate.onChartClick()
        }

        pullToRefresh.setOnRefreshListener {
            viewModel.delegate.refresh()
        }

        observeLiveData()
        setSwipeBackground()
        setAppBarAnimation()
    }

    override fun onResume() {
        super.onResume()
        coinAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerCoins.adapter = null
    }

    // BalanceSort listener

    override fun onSortItemSelect(sortType: BalanceSortType) {
        viewModel.delegate.onSortTypeChanged(sortType)
    }

    private fun setSwipeBackground() {
        activity?.theme?.let { theme ->
            LayoutHelper.getAttr(R.attr.SwipeRefreshBackgroundColor, theme)?.let { color ->
                pullToRefresh.setProgressBackgroundColorSchemeColor(color)
            }
            LayoutHelper.getAttr(R.attr.ColorOz, theme)?.let { color ->
                pullToRefresh.setColorSchemeColors(color)
            }
        }
    }

    private fun setAppBarAnimation() {
        toolbarTitle.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                toolbarTitle.pivotX = 0f
                toolbarTitle.pivotY = toolbarTitle.bottom.toFloat()
                toolbarTitle.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val fraction = Math.abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange
            var alphaFract = 1f - fraction
            if (alphaFract < 0.20) {
                alphaFract = 0f
            }
            toolbarTitle.alpha = alphaFract
            toolbarTitle.scaleX = (1f - fraction / 3)
            toolbarTitle.scaleY = (1f - fraction / 3)
        })
    }

    // BalanceAdapter listener

    override fun onSendClicked(position: Int) {
        viewModel.onSendClicked(position)
    }

    override fun onReceiveClicked(position: Int) {
        viewModel.onReceiveClicked(position)
    }

    override fun onItemClick(position: Int) {
        coinAdapter.toggleViewHolder(position)
    }

    override fun onAddCoinClick() {
        viewModel.delegate.openManageCoins()
    }

    override fun onClickChart(position: Int) {
        viewModel.delegate.openChart(position)
    }

    // LiveData

    private fun observeLiveData() {
        viewModel.openReceiveDialog.observe(viewLifecycleOwner, Observer {
            (activity as? MainActivity)?.openReceiveDialog(it)
        })

        viewModel.openSendDialog.observe(viewLifecycleOwner, Observer {
            (activity as? MainActivity)?.openSend(it)
        })

        viewModel.didRefreshLiveEvent.observe(viewLifecycleOwner, Observer {
            pullToRefresh.isRefreshing = false
        })

        viewModel.openManageCoinsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { ManageWalletsModule.start(it) }
        })

        viewModel.reloadLiveEvent.observe(viewLifecycleOwner, Observer {
            coinAdapter.notifyDataSetChanged()
            reloadHeader()
            if (viewModel.delegate.itemsCount > 0) {
                recyclerCoins.animate().alpha(1f)
            }
        })

        viewModel.reloadHeaderLiveEvent.observe(viewLifecycleOwner, Observer {
            reloadHeader()
        })

        viewModel.reloadItemLiveEvent.observe(viewLifecycleOwner, Observer { position ->
            coinAdapter.notifyItemChanged(position)
        })

        viewModel.openSortingTypeDialogLiveEvent.observe(viewLifecycleOwner, Observer { sortingType ->
            BalanceSortDialogFragment.newInstance(this, sortingType).also { it.show(childFragmentManager, it.tag) }
        })

        viewModel.setSortingOnLiveEvent.observe(viewLifecycleOwner, Observer { visible ->
            sortButton.visibility = if (visible) View.VISIBLE else View.GONE
        })

        viewModel.setChartOnLiveEvent.observe(viewLifecycleOwner, Observer { visible ->
            chartButton.visibility = if (visible) View.VISIBLE else View.GONE
        })

        viewModel.showBackupAlert.observe(viewLifecycleOwner, Observer {
            activity?.let { activity ->
                BackupAlertDialog.show(activity, getString(it.second.title), it.first.title, object : BackupAlertDialog.Listener {
                    override fun onBackupButtonClick() {
                        viewModel.delegate.openBackup()
                    }
                })
            }
        })

        viewModel.openBackup.observe(viewLifecycleOwner, Observer { (account, coinCodesStringRes) ->
            context?.let { BackupModule.start(it, account, getString(coinCodesStringRes)) }
        })

        viewModel.openChartModule.observe(viewLifecycleOwner, Observer { coin ->
            RateChartFragment(coin).also { it.show(childFragmentManager, it.tag) }
        })

        viewModel.setChartButtonEnabled.observe(viewLifecycleOwner, Observer { enabled ->
            chartButton.isActivated = enabled
        })
    }

    private fun reloadHeader() {
        val headerViewItem = viewModel.delegate.getHeaderViewItem()

        context?.let {
            val color = if (headerViewItem.upToDate) R.color.yellow_crypto else R.color.yellow_crypto_40
            ballanceText.setTextColor(ContextCompat.getColor(it, color))
        }

        ballanceText.text = headerViewItem.currencyValue?.let {
            App.numberFormatter.format(it)
        }
    }
}
