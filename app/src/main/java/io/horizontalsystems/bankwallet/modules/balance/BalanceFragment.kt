package io.horizontalsystems.bankwallet.modules.balance

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.SimpleItemAnimator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backupkey.BackupKeyModule
import io.horizontalsystems.bankwallet.modules.balance.views.SyncErrorDialog
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.receive.ReceiveFragment
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.swap.SwapFragment
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_balance.*

class BalanceFragment : BaseFragment(), BalanceItemsAdapter.Listener, BackupRequiredDialog.Listener {

    private val viewModel by navGraphViewModels<BalanceViewModel2>(R.id.mainFragment) { BalanceModule.Factory() }
    private val balanceItemsAdapter = BalanceItemsAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerCoins.adapter = balanceItemsAdapter
        recyclerCoins.layoutManager = NpaLinearLayoutManager(context)
        (recyclerCoins.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        sortButton.setOnClickListener {
            val sortTypes = listOf(BalanceSortType.Name, BalanceSortType.Value, BalanceSortType.PercentGrowth)
            val selectorItems = sortTypes.map {
                SelectorItem(getString(it.getTitleRes()), it == viewModel.sortType)
            }
            SelectorDialog
                .newInstance(selectorItems, getString(R.string.Balance_Sort_PopupTitle)) { position ->
                    viewModel.sortType = sortTypes[position]
                }
                .show(parentFragmentManager, "balance_sort_type_selector")
        }

        pullToRefresh.setOnRefreshListener {
            viewModel.onRefresh()

            Handler(Looper.getMainLooper()).postDelayed({
                pullToRefresh.isRefreshing = false
            }, 1000)
        }

        toolbarTitle.setOnSingleClickListener {
            ManageAccountsModule.start(this, R.id.mainFragment_to_manageKeysFragment, navOptionsFromBottom(), ManageAccountsModule.Mode.Switcher)
        }

        balanceText.setOnClickListener {
            viewModel.onBalanceClick()
            HudHelper.vibrate(requireContext())
        }

        addCoinButton.setOnClickListener {
            findNavController().navigate(R.id.mainFragment_to_manageWalletsFragment, null, navOptions())
        }

        observeLiveData()
        setSwipeBackground()
    }

    override fun onDestroyView() {
        super.onDestroyView()

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

    //  BackupRequiredDialog Listener

    override fun onClickBackup(account: Account) {
        BackupKeyModule.start(this, R.id.mainFragment_to_backupKeyFragment, navOptions(), account)
    }

    // BalanceAdapter listener

    override fun onSendClicked(viewItem: BalanceViewItem) {
        when (viewItem.wallet.coin.type) {
            CoinType.Ethereum, is CoinType.Erc20,
            CoinType.BinanceSmartChain, is CoinType.Bep20 -> {
                findNavController().navigate(
                    R.id.mainFragment_to_sendEvmFragment,
                    bundleOf(SendEvmModule.walletKey to viewItem.wallet),
                    navOptionsFromBottom()
                )
            }
            else -> {
                (activity as? MainActivity)?.openSend(viewItem.wallet)
            }
        }
    }

    override fun onReceiveClicked(viewItem: BalanceViewItem) {
        try {
            findNavController().navigate(R.id.mainFragment_to_receiveFragment, bundleOf(ReceiveFragment.WALLET_KEY to viewModel.getWalletForReceive(viewItem)), navOptionsFromBottom())
        } catch (e: BalanceViewModel2.BackupRequiredError) {
            BackupRequiredDialog.show(childFragmentManager, e.account)
        }
    }

    override fun onSwapClicked(viewItem: BalanceViewItem) {
        findNavController().navigate(R.id.mainFragment_to_swapFragment, bundleOf(SwapFragment.fromCoinKey to viewItem.wallet.coin))
    }

    override fun onItemClicked(viewItem: BalanceViewItem) {
        viewModel.onItem(viewItem)
    }

    override fun onChartClicked(viewItem: BalanceViewItem) {
        val coin = viewItem.wallet.coin
        val arguments = CoinFragment.prepareParams(coin.type, coin.code, coin.title)

        findNavController().navigate(R.id.mainFragment_to_coinFragment, arguments, navOptions())
    }

    override fun onSyncErrorClicked(viewItem: BalanceViewItem) {
        when (val syncErrorDetails = viewModel.getSyncErrorDetails(viewItem)) {
            is BalanceViewModel2.SyncError.Dialog -> {

                val wallet = syncErrorDetails.wallet
                val sourceChangeable = syncErrorDetails.sourceChangeable
                val errorMessage = syncErrorDetails.errorMessage

                activity?.let { fragmentActivity ->
                    SyncErrorDialog.show(fragmentActivity, wallet.coin.title, sourceChangeable, object : SyncErrorDialog.Listener {
                        override fun onClickRetry() {
                            viewModel.refreshByWallet(wallet)
                        }

                        override fun onClickChangeSource() {
                            findNavController().navigate(R.id.mainFragment_to_privacySettingsFragment, null, navOptions())
                        }

                        override fun onClickReport() {
                            sendEmail(viewModel.reportEmail, errorMessage)
                        }
                    })
                }
            }
            is BalanceViewModel2.SyncError.NetworkNotAvailable -> {
                HudHelper.showErrorMessage(this.requireView(), R.string.Hud_Text_NoInternet)
            }
        }


    }

    override fun onAttachFragment(childFragment: Fragment) {
        if (childFragment is BackupRequiredDialog) {
            childFragment.setListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }
    // LiveData

    private fun observeLiveData() {
        viewModel.titleLiveData.observe(viewLifecycleOwner) {
            toolbarTitle.text = it ?: getString(R.string.Balance_Title)
        }

        viewModel.balanceViewItemsLiveData.observe(viewLifecycleOwner) {
            val scrollToTop = balanceItemsAdapter.itemCount == 1
            balanceItemsAdapter.submitList(it) {
                if (scrollToTop) {
                    recyclerCoins?.layoutManager?.scrollToPosition(0)
                }
            }
        }

        viewModel.headerViewItemLiveData.observe(viewLifecycleOwner) {
            setHeaderViewItem(it)
        }
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

    private fun setHeaderViewItem(headerViewItem: BalanceHeaderViewItem) {
        headerViewItem.apply {
            balanceText.text = xBalanceText
            context?.let { context -> balanceText.setTextColor(getBalanceTextColor(context)) }
        }
    }
}
