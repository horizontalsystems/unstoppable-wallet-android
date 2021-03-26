package io.horizontalsystems.bankwallet.ui.extensions.coinlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsModule
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorDialog
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_manage_wallets.*

abstract class CoinListBaseFragment : BaseWithSearchFragment(), CoinListAdapter.Listener {

    private lateinit var featuredItemsAdapter: CoinListAdapter
    private lateinit var itemsAdapter: CoinListAdapter

    abstract val title: CharSequence

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_manage_wallets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.title = title
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        featuredItemsAdapter = CoinListAdapter(this)
        itemsAdapter = CoinListAdapter(this)
        recyclerView.itemAnimator = null
        recyclerView.adapter = ConcatAdapter(featuredItemsAdapter, itemsAdapter)

    }

    // ManageWalletItemsAdapter.Listener

    override fun enable(coin: Coin) {}

    override fun disable(coin: Coin) {}

    override fun select(coin: Coin) {}

    // CoinListBaseFragment

    protected fun setViewState(viewState: CoinViewState) {
        featuredItemsAdapter.submitList(viewState.featuredViewItems)
        itemsAdapter.submitList(viewState.viewItems)

        progressLoading.isVisible = false
    }

    open fun onCancelSelection() {}

    open fun onSelect(index: Int) {}

    protected fun showBottomSelectorDialog(config: BlockchainSettingsModule.Config) {
        val coinDrawable = context?.let { AppLayoutHelper.getCoinDrawable(it, config.coin.type) }

        BottomSheetSelectorDialog.show(
                fragmentManager = childFragmentManager,
                title = config.title,
                subtitle = config.subtitle,
                icon = coinDrawable,
                items = config.viewItems,
                selected = config.selectedIndex,
                notifyUnchanged = true,
                onItemSelected = { onSelect(it) },
                onCancelled = { onCancelSelection() }
        )
    }

}
