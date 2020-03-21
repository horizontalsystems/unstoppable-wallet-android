package io.horizontalsystems.bankwallet.modules.balance

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.SimpleItemAnimator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartActivity
import io.horizontalsystems.bankwallet.modules.receive.ReceiveFragment
import io.horizontalsystems.bankwallet.modules.settings.managekeys.views.ManageKeysDialog
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_balance.*

class BalanceFragment : Fragment(), BalanceCoinAdapter.Listener, BalanceSortDialogFragment.Listener, ReceiveFragment.Listener {

    private lateinit var viewModel: BalanceViewModel
    private val coinAdapter = BalanceCoinAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(BalanceViewModel::class.java)
        viewModel.init()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        recyclerCoins.adapter = coinAdapter
        recyclerCoins.layoutManager = NpaLinearLayoutManager(context)
        (recyclerCoins.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        sortButton.setOnClickListener {
            viewModel.delegate.onSortClick()
        }

        pullToRefresh.setOnRefreshListener {
            viewModel.delegate.onRefresh()
        }

        observeLiveData()
        setSwipeBackground()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerCoins.adapter = null
    }

    // BalanceSort listener

    override fun onSortItemSelect(sortType: BalanceSortType) {
        viewModel.delegate.onSortTypeChange(sortType)
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

    // ReceiveFragment listener
    override fun shareReceiveAddress(address: String) {
        activity?.let {
            ShareCompat.IntentBuilder
                .from(it)
                .setType("text/plain")
                .setText(address)
                .startChooser()
        }
    }

    // BalanceAdapter listener

    override fun onSendClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onPay(viewItem)
    }

    override fun onReceiveClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onReceive(viewItem)
    }

    override fun onItemClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onItem(viewItem)
    }

    override fun onChartClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onChart(viewItem)
    }

    override fun onAddCoinClicked() {
        viewModel.delegate.onAddCoinClick()
    }

    override fun onAttachFragment(childFragment: Fragment) {
        if (childFragment is ReceiveFragment) {
            childFragment.setListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.delegate.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.delegate.onPause()
    }
    // LiveData

    private fun observeLiveData() {
        viewModel.openReceiveDialog.observe(viewLifecycleOwner, Observer { wallet ->
            ReceiveFragment.newInstance(wallet).show(childFragmentManager, "ReceiveFragment")
        })

        viewModel.openSendDialog.observe(viewLifecycleOwner, Observer {
            (activity as? MainActivity)?.openSend(it)
        })

        viewModel.didRefreshLiveEvent.observe(viewLifecycleOwner, Observer {
            Handler().postDelayed({ pullToRefresh.isRefreshing = false }, 1000)
        })

        viewModel.openManageCoinsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { ManageWalletsModule.start(it, true) }
        })

        viewModel.setViewItems.observe(viewLifecycleOwner, Observer {
            coinAdapter.setItems(it)
        })

        viewModel.setHeaderViewItem.observe(viewLifecycleOwner, Observer {
            setHeaderViewItem(it)
        })

        viewModel.openSortingTypeDialogLiveEvent.observe(viewLifecycleOwner, Observer { sortingType ->
            BalanceSortDialogFragment.newInstance(this, sortingType).also { it.show(childFragmentManager, it.tag) }
        })

        viewModel.isSortOn.observe(viewLifecycleOwner, Observer { visible ->
            sortButton.visibility = if (visible) View.VISIBLE else View.GONE
        })

        viewModel.showBackupAlert.observe(viewLifecycleOwner, Observer { (coin, predefinedAccount) ->
            activity?.let { activity ->
                val title = getString(R.string.ManageKeys_Delete_Alert_Title)
                val subtitle = getString(predefinedAccount.title)
                val description = getString(R.string.Balance_Backup_Alert, getString(predefinedAccount.title), coin.title)
                ManageKeysDialog.show(title, subtitle, description, activity, object : ManageKeysDialog.Listener {
                    override fun onClickBackupKey() {
                        viewModel.delegate.onBackupClick()
                    }
                }, ManageKeysDialog.ManageAction.BACKUP)
            }
        })

        viewModel.openBackup.observe(viewLifecycleOwner, Observer { (account, coinCodesStringRes) ->
            context?.let { BackupModule.start(it, account, getString(coinCodesStringRes)) }
        })

        viewModel.openChartModule.observe(viewLifecycleOwner, Observer { coin ->
            startActivity(Intent(activity, RateChartActivity::class.java).apply {
                putExtra("coin", coin)
            })
        })
    }

    private fun setHeaderViewItem(headerViewItem: BalanceHeaderViewItem) {
        headerViewItem.apply {
            balanceText.text = xBalanceText
            context?.let { context -> balanceText.setTextColor(getBalanceTextColor(context)) }
        }
    }
}
