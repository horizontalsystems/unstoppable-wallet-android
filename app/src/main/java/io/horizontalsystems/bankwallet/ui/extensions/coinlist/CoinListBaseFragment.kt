package io.horizontalsystems.bankwallet.ui.extensions.coinlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.MarketCoin
import kotlinx.android.synthetic.main.fragment_manage_wallets.*

abstract class CoinListBaseFragment : BaseWithSearchFragment(), CoinListAdapter.Listener {

    private lateinit var itemsAdapter: CoinListAdapter

    abstract val title: CharSequence

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_manage_wallets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.title = title
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        recyclerView.itemAnimator = null
        recyclerView.adapter = CoinListAdapter(this)
    }

    // ManageWalletItemsAdapter.Listener

    override fun enable(marketCoin: MarketCoin) {}

    override fun disable(marketCoin: MarketCoin) {}


    // CoinListBaseFragment

    protected fun setViewItems(viewItems: List<CoinViewItem>) {
        itemsAdapter.submitList(viewItems)
        progressLoading.isVisible = false
    }

    protected fun disableCoin(coin: Coin) {
        itemsAdapter.disableCoin(coin)
    }

    open fun onCancelSelection() {}

    open fun onSelect(indexes: List<Int>) {}

    protected fun showBottomSelectorDialog(config: CoinSettingsViewModel.Config) {
        val coinDrawable = context?.let { AppLayoutHelper.getCoinDrawable(it, config.platformCoin.coinType) }

        BottomSheetSelectorMultipleDialog.show(
            fragmentManager = childFragmentManager,
            title = config.title,
            subtitle = config.subtitle,
            icon = coinDrawable,
            items = config.viewItems,
            selected = config.selectedIndexes,
            notifyUnchanged = true,
            onItemSelected = { onSelect(it) },
            onCancelled = { onCancelSelection() },
            warning = config.description
        )
    }

}
