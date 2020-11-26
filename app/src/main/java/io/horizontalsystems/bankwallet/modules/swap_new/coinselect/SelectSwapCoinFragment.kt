package io.horizontalsystems.bankwallet.modules.swap_new.coinselect

import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule.CoinBalanceItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult
import kotlinx.android.synthetic.main.fragment_swap_select_token.*

class SelectSwapCoinFragment : BaseWithSearchFragment() {

    private var viewModel: SelectSwapCoinViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap_select_token, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.title = getString(R.string.ManageCoins_title)
        }

        val coinBalanceItems = arguments?.getParcelableArrayList<CoinBalanceItem>(coinBalanceItemsListKey)
        val requestId = arguments?.getInt(requestIdKey)
        if (coinBalanceItems == null || requestId == null) {
            findNavController().popBackStack()
            return
        }

        viewModel = ViewModelProvider(this, SelectSwapCoinModule.Factory(coinBalanceItems))
                .get(SelectSwapCoinViewModel::class.java)

        val adapter = SelectSwapCoinAdapter(onClickItem = { closeWithResult(it, requestId) })

        recyclerView.adapter = adapter

        viewModel?.coinItemsLivedData?.observe(viewLifecycleOwner, { items ->
            adapter.items = items
            adapter.notifyDataSetChanged()
        })

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.select_swap_coin_menu, menu)
        configureSearchMenu(menu, R.string.ManageCoins_Search)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                findNavController().popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun updateFilter(query: String) {
        viewModel?.updateFilter(query)
    }

    private fun closeWithResult(coinBalanceItem: CoinBalanceItem, requestId: Int) {
        hideKeyboard()
        setNavigationResult(resultBundleKey, bundleOf(
                requestIdKey to requestId,
                coinBalanceItemResultKey to coinBalanceItem
        ))
        Handler().postDelayed({
            findNavController().popBackStack()
        }, 100)
    }

    companion object {
        const val resultBundleKey = "selectSwapCoinResultKey"
        const val coinBalanceItemsListKey = "coinBalanceItemsListKey"
        const val requestIdKey = "requestIdKey"
        const val coinBalanceItemResultKey = "coinBalanceItemResultKey"

        fun params(requestId: Int, coinBalanceItems: List<CoinBalanceItem>): Bundle {
            return bundleOf(
                    requestIdKey to requestId,
                    coinBalanceItemsListKey to coinBalanceItems
            )
        }
    }

}
