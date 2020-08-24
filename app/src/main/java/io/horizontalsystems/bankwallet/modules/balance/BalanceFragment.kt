package io.horizontalsystems.bankwallet.modules.balance

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.app.ShareCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.SimpleItemAnimator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.balance.views.SyncErrorDialog
import io.horizontalsystems.bankwallet.modules.settings.contact.ContactModule
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartActivity
import io.horizontalsystems.bankwallet.modules.receive.ReceiveFragment
import io.horizontalsystems.bankwallet.modules.settings.managekeys.views.ManageKeysDialog
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsModule
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.getValueAnimator
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.measureHeight
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_balance.*

class BalanceFragment : Fragment(), BalanceItemsAdapter.Listener, ReceiveFragment.Listener, ManageKeysDialog.Listener {

    private val viewModel by viewModels<BalanceViewModel> { BalanceModule.Factory() }
    private val balanceItemsAdapter = BalanceItemsAdapter(this)
    private var totalBalanceTabHeight: Int = 0
    private val animationPlaybackSpeed: Double = 1.3
    private val expandDuration: Long get() = (300L / animationPlaybackSpeed).toLong()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        totalBalanceTabHeight = balanceTabWrapper.measureHeight()

        recyclerCoins.adapter = balanceItemsAdapter
        recyclerCoins.layoutManager = NpaLinearLayoutManager(context)
        (recyclerCoins.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        sortButton.setOnClickListener {
            viewModel.delegate.onSortClick()
        }

        pullToRefresh.setOnRefreshListener {
            viewModel.delegate.onRefresh()
        }

        totalBalanceWrapper.setOnClickListener { viewModel.delegate.onHideBalanceClick() }

        observeLiveData()
        setSwipeBackground()
    }

    private fun setOptionsMenuVisible(visible: Boolean) {
        if (visible) {
            toolbar.menu.clear()
            toolbar.inflateMenu(R.menu.balance_menu)
            toolbar.setOnMenuItemClickListener { item: MenuItem? ->
                if (item?.itemId == R.id.menuShowBalance) {
                    viewModel.delegate.onShowBalanceClick()
                    return@setOnMenuItemClickListener true
                }
                return@setOnMenuItemClickListener false
            }
        } else {
            toolbar.menu.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerCoins.adapter = null
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

    //  ManageKeysDialog Listener

    override fun onClickBackupKey() {
        viewModel.delegate.onBackupClick()
    }

    // BalanceAdapter listener

    override fun onSendClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onPay(viewItem)
    }

    override fun onReceiveClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onReceive(viewItem)
    }

    override fun onSwapClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onSwap(viewItem)
    }

    override fun onItemClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onItem(viewItem)
    }

    override fun onChartClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onChart(viewItem)
    }

    override fun onSyncErrorClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onSyncErrorClick(viewItem)
    }

    override fun onAddCoinClicked() {
        viewModel.delegate.onAddCoinClick()
    }

    override fun onAttachFragment(childFragment: Fragment) {
        if (childFragment is ReceiveFragment) {
            childFragment.setListener(this)
        } else if (childFragment is ManageKeysDialog) {
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

        viewModel.openSwap.observe(viewLifecycleOwner, Observer { wallet ->
            context?.let { context -> SwapModule.start(context, wallet.coin) }
        })

        viewModel.didRefreshLiveEvent.observe(viewLifecycleOwner, Observer {
            Handler().postDelayed({ pullToRefresh.isRefreshing = false }, 1000)
        })

        viewModel.openManageCoinsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { ManageWalletsModule.start(it) }
        })

        viewModel.setViewItems.observe(viewLifecycleOwner, Observer {
            val scrollToTop = balanceItemsAdapter.itemCount == 1
            balanceItemsAdapter.submitList(it) {
                if (scrollToTop) {
                    recyclerCoins.layoutManager?.scrollToPosition(0)
                }
            }
        })

        viewModel.setHeaderViewItem.observe(viewLifecycleOwner, Observer {
            setHeaderViewItem(it)
        })

        viewModel.openSortingTypeDialogLiveEvent.observe(viewLifecycleOwner, Observer { selected ->
            val sortTypes = listOf(BalanceSortType.Name, BalanceSortType.Value, BalanceSortType.PercentGrowth)
            val selectorItems = sortTypes.map {
                SelectorItem(getString(it.getTitleRes()), it == selected)
            }
            SelectorDialog
                    .newInstance(selectorItems, getString(R.string.Balance_Sort_PopupTitle)) { position ->
                        viewModel.delegate.onSortTypeChange(sortTypes[position])
                    }
                    .show(parentFragmentManager, "balance_sort_type_selector")
        })

        viewModel.isSortOn.observe(viewLifecycleOwner, Observer { visible ->
            sortButton.isVisible = visible
        })

        viewModel.showBackupAlert.observe(viewLifecycleOwner, Observer { (coin, predefinedAccount) ->
            val title = getString(R.string.ManageKeys_Delete_Alert_Title)
            val subtitle = getString(predefinedAccount.title)
            val description = getString(R.string.Balance_Backup_Alert, getString(predefinedAccount.title), coin.title)
            ManageKeysDialog.show(childFragmentManager, title, subtitle, description)
        })

        viewModel.openBackup.observe(viewLifecycleOwner, Observer { (account, coinCodesStringRes) ->
            context?.let { BackupModule.start(it, account, getString(coinCodesStringRes)) }
        })

        viewModel.openChartModule.observe(viewLifecycleOwner, Observer { coin ->
            startActivity(Intent(activity, RateChartActivity::class.java).apply {
                putExtra(ModuleField.COIN_ID, coin.coinId)
                putExtra(ModuleField.COIN_CODE, coin.code)
                putExtra(ModuleField.COIN_TITLE, coin.title)
            })
        })

        viewModel.openContactPage.observe(viewLifecycleOwner, Observer {
            activity?.let {
                ContactModule.start(it)
            }
        })

        viewModel.setBalanceHidden.observe(viewLifecycleOwner, Observer { (hideBalance, animate) ->
            setOptionsMenuVisible(hideBalance)

            if (animate) {
                val animator = getValueAnimator(!hideBalance, expandDuration, AccelerateDecelerateInterpolator()
                ) { progress -> setExpandProgress(balanceTabWrapper, 0, totalBalanceTabHeight, progress) }
                animator.start()
            } else {
                setExpandProgress(balanceTabWrapper, 0, totalBalanceTabHeight, if (hideBalance) 0f else 1f)
            }
        })

        viewModel.showSyncError.observe(viewLifecycleOwner, Observer { (wallet, errorMessage, sourceChangeable) ->
            activity?.let { fragmentActivity ->
                SyncErrorDialog.show(fragmentActivity, wallet.coin.title, sourceChangeable, object : SyncErrorDialog.Listener {
                    override fun onClickRetry() {
                        viewModel.delegate.refreshByWallet(wallet)
                    }

                    override fun onClickChangeSource() {
                        PrivacySettingsModule.start(fragmentActivity)
                    }

                    override fun onClickReport() {
                        viewModel.delegate.onReportClick(errorMessage)
                    }

                })
            }
        })

        viewModel.networkNotAvailable.observe(viewLifecycleOwner, Observer {
            HudHelper.showErrorMessage(this.requireView(), R.string.Hud_Text_NoInternet)
        })

        viewModel.showErrorMessageCopied.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(this.requireView(), R.string.Hud_Text_Copied)
        })
    }

    private fun setExpandProgress(view: View, smallHeight: Int, bigHeight: Int, progress: Float) {
        view.layoutParams.height = (smallHeight + (bigHeight - smallHeight) * progress).toInt()
        view.requestLayout()
    }

    private fun setHeaderViewItem(headerViewItem: BalanceHeaderViewItem) {
        headerViewItem.apply {
            balanceText.text = xBalanceText
            context?.let { context -> balanceText.setTextColor(getBalanceTextColor(context)) }
        }
    }
}
