package io.horizontalsystems.bankwallet.ui.extensions.coinlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.databinding.FragmentManageWalletsBinding
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import java.util.concurrent.atomic.AtomicBoolean

abstract class CoinListBaseFragment : BaseWithSearchFragment(), CoinListAdapter.Listener {

    private lateinit var itemsAdapter: CoinListAdapter

    abstract val title: CharSequence
    protected var scrollToTopAfterUpdate = false

    private var _binding: FragmentManageWalletsBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageWalletsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        itemsAdapter = CoinListAdapter(this)
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = itemsAdapter
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
        binding.toolbar.menu.findItem(R.id.menuAddToken)?.isVisible =
            !searchExpanded.get() || searchExpanded.get() && viewItems.isEmpty()

        itemsAdapter.submitList(viewItems) {
            if (scrollToTopAfterUpdate) {
                binding.recyclerView.scrollToPosition(0)
                scrollToTopAfterUpdate = false
            }
        }
        binding.progressLoading.isVisible = false
        binding.noResultsText.isVisible = viewItems.isEmpty()
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
