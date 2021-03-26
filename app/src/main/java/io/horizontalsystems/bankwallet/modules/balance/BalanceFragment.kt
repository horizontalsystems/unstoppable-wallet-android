package io.horizontalsystems.bankwallet.modules.balance

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.app.ShareCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.SimpleItemAnimator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.balance.views.SyncErrorDialog
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.receive.ReceiveFragment
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.settings.managekeys.views.ManageKeysDialog
import io.horizontalsystems.bankwallet.modules.swap.SwapFragment
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getValueAnimator
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.measureHeight
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_balance.*

class BalanceFragment : BaseFragment(), BalanceItemsAdapter.Listener, ReceiveFragment.Listener, ManageKeysDialog.Listener {

    private val viewModel by viewModels<BalanceViewModel> { BalanceModule.Factory() }
    private val balanceItemsAdapter = BalanceItemsAdapter(this)
    private var totalBalanceTabHeight: Int = 0
    private val animationPlaybackSpeed: Double = 1.3
    private val expandDuration: Long get() = (300L / animationPlaybackSpeed).toLong()
    private var showBalanceMenuItem: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuShowBalance -> {
                    viewModel.delegate.onShowBalanceClick()
                    true
                }
                else -> false
            }
        }
        showBalanceMenuItem = toolbar.menu.findItem(R.id.menuShowBalance)

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

    override fun onDestroyView() {
        super.onDestroyView()

        showBalanceMenuItem = null
        recyclerCoins.adapter = null
        recyclerCoins.layoutManager = null
    }

    private fun setSwipeBackground() {
        activity?.theme?.let { theme ->
            LayoutHelper.getAttr(R.attr.SwipeRefreshBackgroundColor, theme)?.let { color ->
                pullToRefresh.setProgressBackgroundColorSchemeColor(color)
            }
            pullToRefresh.setColorSchemeColors(requireContext().getColor(R.color.oz))
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

        viewModel.openSendEvmDialog.observe(viewLifecycleOwner, { wallet ->
            findNavController().navigate(R.id.mainFragment_to_sendEvmFragment, bundleOf(SendEvmModule.walletKey to wallet), navOptionsFromBottom())
        })

        viewModel.openSwap.observe(viewLifecycleOwner, Observer { wallet ->
            findNavController().navigate(R.id.mainFragment_to_swapFragment, bundleOf(SwapFragment.fromCoinKey to wallet.coin))
        })

        viewModel.didRefreshLiveEvent.observe(viewLifecycleOwner, Observer {
            Handler(Looper.getMainLooper()).postDelayed({
                if (view != null) {
                    pullToRefresh.isRefreshing = false
                }
            }, 1000)
        })

        viewModel.openManageCoinsLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.mainFragment_to_manageWalletsFragment, null, navOptions())
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
            val arguments = Bundle(2).apply {
                putParcelable(ModuleField.ACCOUNT, account)
                putString(ModuleField.ACCOUNT_COINS, getString(coinCodesStringRes))
            }

            findNavController().navigate(R.id.mainFragment_to_backupFragment, arguments, navOptions())
        })

        viewModel.openChartModule.observe(viewLifecycleOwner, Observer { coin ->
            val arguments = CoinFragment.prepareParams(coin.type, coin.code, coin.title)

            findNavController().navigate(R.id.mainFragment_to_coinFragment, arguments, navOptions())
        })

        viewModel.openEmail.observe(viewLifecycleOwner, Observer { (email, report) ->
            sendEmail(email, report)
        })

        viewModel.setBalanceHidden.observe(viewLifecycleOwner, Observer { (hideBalance, animate) ->
            showBalanceMenuItem?.isVisible = hideBalance

            if (animate) {
                val animator = getValueAnimator(!hideBalance, expandDuration, AccelerateDecelerateInterpolator()) { progress ->
                    setExpandProgress(balanceTabWrapper, 0, totalBalanceTabHeight, progress)
                }
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
                        findNavController().navigate(R.id.mainFragment_to_privacySettingsFragment, null, navOptions())
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

    }

    private fun sendEmail(email: String, report: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_TEXT, report)
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            TextHelper.copyText(email)
            HudHelper.showSuccessMessage(this.requireView(), R.string.Hud_Text_EmailAddressCopied)
        }
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
