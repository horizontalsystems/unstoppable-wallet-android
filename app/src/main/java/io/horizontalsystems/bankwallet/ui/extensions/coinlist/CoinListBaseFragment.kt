package io.horizontalsystems.bankwallet.ui.extensions.coinlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.android.synthetic.main.fragment_manage_wallets.*
import java.util.concurrent.atomic.AtomicBoolean

abstract class CoinListBaseFragment : BaseWithSearchFragment(), CoinListAdapter.Listener {

    private lateinit var itemsAdapter: CoinListAdapter

    abstract val title: CharSequence
    protected var scrollToTopAfterUpdate = false

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

        itemsAdapter = CoinListAdapter(this)
        recyclerView.itemAnimator = null
        recyclerView.adapter = itemsAdapter
    }

    // ManageWalletItemsAdapter.Listener

    override fun enable(fullCoin: FullCoin) {}

    override fun disable(fullCoin: FullCoin) {}


    // ManageWalletItemsAdapter.Listener

    private var searchExpanded = AtomicBoolean(false)

    override fun searchExpanded(menu: Menu) {
        searchExpanded.set(true)
    }

    override fun searchCollapsed(menu: Menu) {
        searchExpanded.set(false)
    }


    // CoinListBaseFragment

    protected fun setViewItems(viewItems: List<CoinViewItem>) {
        toolbar.menu.findItem(R.id.menuAddToken)?.isVisible =
            !searchExpanded.get() || searchExpanded.get() && viewItems.isEmpty()

        itemsAdapter.submitList(viewItems) {
            if (scrollToTopAfterUpdate) {
                recyclerView.scrollToPosition(0)
                scrollToTopAfterUpdate = false
            }
        }
        progressLoading.isVisible = false
        noResultsText.isVisible = viewItems.isEmpty()
    }

    protected fun disableCoin(coin: Coin) {
        itemsAdapter.disableCoin(coin)
    }

    protected fun showBottomSelectorDialog(
        config: BottomSheetSelectorMultipleDialog.Config,
        onSelect: (indexes: List<Int>) -> Unit,
        onCancel: () -> Unit
    ) {
        BottomSheetSelectorMultipleDialog.show(
            fragmentManager = childFragmentManager,
            title = config.title,
            subtitle = config.subtitle,
            icon = config.icon,
            items = config.viewItems,
            selected = config.selectedIndexes,
            notifyUnchanged = true,
            onItemSelected = { onSelect(it) },
            onCancelled = { onCancel() },
            warning = config.description
        )
    }

}
