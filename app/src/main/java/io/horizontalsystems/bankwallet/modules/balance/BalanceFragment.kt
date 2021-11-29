package io.horizontalsystems.bankwallet.modules.balance

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.material.snackbar.Snackbar
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backupkey.BackupKeyModule
import io.horizontalsystems.bankwallet.modules.balance.views.SyncErrorDialog
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkModule
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.receive.ReceiveFragment
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.RotatingCircleProgressView
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.CoinType
import kotlinx.android.synthetic.main.fragment_balance.*
import kotlinx.coroutines.launch

class BalanceFragment : BaseFragment(), BackupRequiredDialog.Listener {

    private val viewModel by viewModels<BalanceViewModel> { BalanceModule.Factory() }
    private var scrollToTopAfterUpdate = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    @ExperimentalAnimationApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbarTitle.setOnSingleClickListener {
            ManageAccountsModule.start(
                this,
                R.id.mainFragment_to_manageKeysFragment,
                navOptionsFromBottom(),
                ManageAccountsModule.Mode.Switcher
            )
        }

        balanceText.setOnClickListener {
            viewModel.onBalanceClick()
            HudHelper.vibrate(requireContext())
        }

        observeLiveData()
        //setSwipeBackground()

        buttonsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        walletListCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setWallets()
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

    //  BackupRequiredDialog Listener

    override fun onClickBackup(account: Account) {
        BackupKeyModule.start(this, R.id.mainFragment_to_backupKeyFragment, navOptions(), account)
    }

    @ExperimentalAnimationApi
    private fun setWallets() {
        walletListCompose.setContent {
            ComposeAppTheme {
                val balanceItems by viewModel.balanceViewItems.observeAsState()
                Wallets(balanceItems)
            }
        }
    }

    @ExperimentalAnimationApi
    @Composable
    fun Wallets(balanceViewItems: List<BalanceViewItem>?) {
        val isRefreshing by viewModel.isRefreshing.observeAsState()

        val coroutineScope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        HSSwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing ?: false),
            onRefresh = { viewModel.onRefresh() }
        ) {
            balanceViewItems?.let {
                LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 18.dp)) {
                    items(it) { item ->
                        WalletCard(
                            viewItem = item,
                        )
                    }
                    if (scrollToTopAfterUpdate) {
                        scrollToTopAfterUpdate = false
                        coroutineScope.launch {
                            listState.scrollToItem(0)
                        }
                    }
                }
            }
        }
    }

    @ExperimentalAnimationApi
    @Composable
    fun WalletCard(viewItem: BalanceViewItem) {
        val ctx = context ?: return
        val interactionSource = remember { MutableInteractionSource() }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 0.dp,
            shape = RoundedCornerShape(16.dp),
            backgroundColor = ComposeAppTheme.colors.lawrence,
        ) {
            Column(
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { viewModel.onItem(viewItem) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LeftIcon(viewItem, ctx)
                    Column {
                        FirstRow(viewItem)
                        SecondRow(viewItem)
                    }
                }
                ExpandableContent(viewItem = viewItem)
            }
        }
    }

    @Composable
    private fun LeftIcon(viewItem: BalanceViewItem, ctx: Context) {
        Box(
            modifier = Modifier.width(56.dp).fillMaxHeight(),
        ) {
            if (!viewItem.mainNet) {
                Image(
                    modifier = Modifier.align(Alignment.TopCenter),
                    painter = painterResource(R.drawable.testnet),
                    contentDescription = "Testnet"
                )
            }
            viewItem.syncingProgress.progress?.let { progress ->
                AndroidView(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(41.dp),
                    factory = { context ->
                        RotatingCircleProgressView(context)
                    },
                    update = { view ->
                        val color = when (viewItem.syncingProgress.dimmed) {
                            true -> R.color.grey_50
                            false -> R.color.grey
                        }
                        view.setProgressColored(progress, view.context.getColor(color))
                    }
                )
            }
            if (viewItem.failedIconVisible) {
                Image(
                    modifier = Modifier.align(Alignment.Center).size(24.dp)
                        .clickable { onSyncErrorClicked(viewItem) },
                    painter = painterResource(id = R.drawable.ic_attention_24),
                    contentDescription = "coin icon",
                    colorFilter = ColorFilter.tint(ComposeAppTheme.colors.lucian)
                )
            } else {
                CoinImage(
                    iconUrl = viewItem.coinIconUrl,
                    placeholder = viewItem.coinIconPlaceholder,
                    modifier = Modifier.align(Alignment.Center).size(24.dp)
                )
            }
        }
    }

    @Composable
    private fun FirstRow(viewItem: BalanceViewItem) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = viewItem.coinCode,
                color = ComposeAppTheme.colors.oz,
                style = ComposeAppTheme.typography.headline2,
                maxLines = 1,
            )
            if (!viewItem.badge.isNullOrBlank()) {
                Box(
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ComposeAppTheme.colors.jeremy)
                ) {
                    Text(
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 1.dp),
                        text = viewItem.badge,
                        color = ComposeAppTheme.colors.bran,
                        style = ComposeAppTheme.typography.microSB,
                        maxLines = 1,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (viewItem.fiatValue.visible) {
                Text(
                    text = viewItem.fiatValue.text ?: "",
                    color = if (viewItem.fiatValue.dimmed) ComposeAppTheme.colors.yellow50 else ComposeAppTheme.colors.jacob,
                    style = ComposeAppTheme.typography.headline2,
                    maxLines = 1,
                )
            }
        }
    }

    @Composable
    private fun SecondRow(viewItem: BalanceViewItem) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier.weight(1f),
            ) {
                if (viewItem.syncingTextValue.visible) {
                    Text(
                        text = viewItem.syncingTextValue.text ?: "",
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.subhead2,
                        maxLines = 1,
                    )
                }
                if (viewItem.exchangeValue.visible) {
                    Row {
                        Text(
                            text = viewItem.exchangeValue.text ?: "",
                            color = if (viewItem.exchangeValue.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.subhead2,
                            maxLines = 1,
                        )
                        Text(
                            modifier = Modifier.padding(start = 4.dp),
                            text = RateText(viewItem.diff),
                            color = RateColor(viewItem.diff),
                            style = ComposeAppTheme.typography.subhead2,
                            maxLines = 1,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.padding(start = 16.dp),
            ) {
                if (viewItem.syncedUntilTextValue.visible) {
                    Text(
                        text = viewItem.syncedUntilTextValue.text ?: "",
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.subhead2,
                        maxLines = 1,
                    )
                }
                if (viewItem.coinValue.visible) {
                    Text(
                        text = viewItem.coinValue.text ?: "",
                        color = if (viewItem.coinValue.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.subhead2,
                        maxLines = 1,
                    )
                }
            }
        }
    }

    @ExperimentalAnimationApi
    @Composable
    private fun ExpandableContent(viewItem: BalanceViewItem) {

        val enterExpand = remember {
            expandVertically(animationSpec = tween(EXPAND_ANIMATION_DURATION))
        }

        val exitCollapse = remember {
            shrinkVertically(animationSpec = tween(COLLAPSE_ANIMATION_DURATION))
        }

        AnimatedVisibility(
            visible = viewItem.expanded,
            enter = enterExpand,
            exit = exitCollapse
        ) {
            Column {
                LockedValueRow(viewItem)
                Divider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10
                )
                ButtonsRow(viewItem)
            }
        }
    }

    @Composable
    private fun LockedValueRow(viewItem: BalanceViewItem) {
        if (viewItem.coinValueLocked.visible) {
            Divider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            Row(
                modifier = Modifier.height(36.dp).padding(start = 16.dp, end = 17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_lock_16),
                    contentDescription = "lock icon"
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = viewItem.coinValueLocked.text ?: "",
                    color = if (viewItem.coinValueLocked.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = viewItem.fiatValueLocked.text ?: "",
                    color = if (viewItem.fiatValueLocked.dimmed) ComposeAppTheme.colors.yellow50 else ComposeAppTheme.colors.jacob,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
            }
        }
    }

    @Composable
    private fun ButtonsRow(viewItem: BalanceViewItem) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 2.dp).height(70.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonPrimaryYellow(
                modifier = Modifier.weight(1f),
                title = getString(R.string.Balance_Send),
                onClick = { onSendClicked(viewItem) },
                enabled = viewItem.sendEnabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (viewItem.swapVisible) {
                ButtonPrimaryCircle(
                    icon = R.drawable.ic_arrow_down_left_24,
                    onClick = { onReceiveClicked(viewItem) },
                    enabled = viewItem.receiveEnabled
                )
                Spacer(modifier = Modifier.width(8.dp))
                ButtonPrimaryCircle(
                    icon = R.drawable.ic_swap_24,
                    onClick = { onSwapClicked(viewItem) },
                    enabled = viewItem.swapEnabled
                )
            } else {
                ButtonPrimaryDefault(
                    modifier = Modifier.weight(1f),
                    title = getString(R.string.Balance_Receive),
                    onClick = { onReceiveClicked(viewItem) },
                    enabled = viewItem.receiveEnabled
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            ButtonPrimaryCircle(
                icon = R.drawable.ic_chart_24,
                onClick = { onChartClicked(viewItem) },
                enabled = viewItem.exchangeValue.text != null
            )
        }
    }

    private fun onSendClicked(viewItem: BalanceViewItem) {
        when (viewItem.wallet.coinType) {
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

    private fun onReceiveClicked(viewItem: BalanceViewItem) {
        try {
            findNavController().navigate(
                R.id.mainFragment_to_receiveFragment,
                bundleOf(ReceiveFragment.WALLET_KEY to viewModel.getWalletForReceive(viewItem)),
                navOptionsFromBottom()
            )
        } catch (e: BalanceViewModel.BackupRequiredError) {
            BackupRequiredDialog.show(childFragmentManager, e.account)
        }
    }

    private fun onSwapClicked(viewItem: BalanceViewItem) {
        SwapMainModule.start(this, navOptionsFromBottom(), viewItem.wallet.platformCoin)
    }

    private fun onChartClicked(viewItem: BalanceViewItem) {
        val platformCoin = viewItem.wallet.platformCoin
        val arguments = CoinFragment.prepareParams(platformCoin.coin.uid)

        findNavController().navigate(R.id.mainFragment_to_coinFragment, arguments, navOptions())
    }

    private fun onSyncErrorClicked(viewItem: BalanceViewItem) {
        when (val syncErrorDetails = viewModel.getSyncErrorDetails(viewItem)) {
            is BalanceViewModel.SyncError.Dialog -> {

                val wallet = syncErrorDetails.wallet
                val sourceChangeable = syncErrorDetails.sourceChangeable
                val errorMessage = syncErrorDetails.errorMessage

                activity?.let { fragmentActivity ->
                    SyncErrorDialog.show(
                        fragmentActivity,
                        wallet.coin.name,
                        sourceChangeable,
                        object : SyncErrorDialog.Listener {
                            override fun onClickRetry() {
                                viewModel.refreshByWallet(wallet)
                            }

                            override fun onClickChangeSource() {
                                viewModel.onChangeSourceClick(wallet)
                            }

                            override fun onClickReport() {
                                sendEmail(viewModel.reportEmail, errorMessage)
                            }
                        })
                }
            }
            is BalanceViewModel.SyncError.NetworkNotAvailable -> {
                HudHelper.showErrorMessage(this.requireView(), R.string.Hud_Text_NoInternet)
            }
        }
    }

    // LiveData

    private fun observeLiveData() {
        viewModel.titleLiveData.observe(viewLifecycleOwner) {
            toolbarTitle.text = it ?: getString(R.string.Balance_Title)
        }

        viewModel.disabledWalletLiveData.observe(viewLifecycleOwner) { wallet ->
            Snackbar.make(
                requireView(),
                getString(R.string.Balance_CoinDisabled, wallet.coin.name),
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.Action_Undo) {
                    viewModel.enable(wallet)
                }
                .show()
        }

        viewModel.headerViewItemLiveData.observe(viewLifecycleOwner) {
            setHeaderViewItem(it)
        }

        viewModel.sortTypeUpdatedLiveData.observe(viewLifecycleOwner, { sortType ->
            setTopButtons(sortType)
        })

        viewModel.openPrivacySettingsLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(
                R.id.mainFragment_to_privacySettingsFragment,
                null,
                navOptions()
            )
        })

        viewModel.openEvmNetworkSettingsLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(
                R.id.evmNetworkFragment,
                EvmNetworkModule.args(it.first, it.second),
                navOptions()
            )
        })
    }

    private fun setTopButtons(sortType: BalanceSortType) {
        buttonsCompose.setContent {
            ComposeAppTheme {
                Row(
                    modifier = Modifier.width(IntrinsicSize.Max).padding(end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ButtonSecondaryTransparent(
                        title = getString(sortType.getTitleRes()),
                        iconRight = R.drawable.ic_down_arrow_20,
                        onClick = {
                            onSortButtonClick(sortType)
                        }
                    )
                    ButtonSecondaryCircle(
                        icon = R.drawable.ic_manage_2,
                        onClick = {
                            findNavController().navigate(
                                R.id.mainFragment_to_manageWalletsFragment, null, navOptions()
                            )
                        }
                    )
                }
            }
        }
    }

    private fun onSortButtonClick(currentSortType: BalanceSortType) {
        val sortTypes =
            listOf(BalanceSortType.Value, BalanceSortType.Name, BalanceSortType.PercentGrowth)
        val selectorItems = sortTypes.map {
            SelectorItem(getString(it.getTitleRes()), it == currentSortType)
        }
        SelectorDialog
            .newInstance(selectorItems, getString(R.string.Balance_Sort_PopupTitle)) { position ->
                viewModel.setSortType(sortTypes[position])
                scrollToTopAfterUpdate = true
            }
            .show(parentFragmentManager, "balance_sort_type_selector")
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

    companion object {
        const val EXPAND_ANIMATION_DURATION = 250
        const val COLLAPSE_ANIMATION_DURATION = 250
    }
}
