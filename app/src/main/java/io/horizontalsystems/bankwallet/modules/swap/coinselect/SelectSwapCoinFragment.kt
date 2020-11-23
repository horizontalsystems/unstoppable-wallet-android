package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.view.SwapFragment
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
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val excludedCoin = arguments?.getParcelable<Coin>(EXCLUDED_COIN_KEY)
        val hideZeroBalance = arguments?.getBoolean(HIDE_ZERO_BALANCE_KEY)
        val selectType = arguments?.getParcelable<SwapFragment.SelectType>(SELECT_TYPE_KEY) ?: run {
            findNavController().popBackStack()
            return
        }

        viewModel = ViewModelProvider(this, SelectSwapCoinModule.Factory(excludedCoin, hideZeroBalance))
                .get(SelectSwapCoinViewModel::class.java)

        val adapter = SelectSwapCoinAdapter(onClickItem = { closeWithResult(it.coin, selectType) })

        recyclerView.adapter = adapter

        viewModel?.coinItemsLivedData?.observe(viewLifecycleOwner, Observer { items ->
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

    private fun closeWithResult(coin: Coin, selectType: SwapFragment.SelectType) {
        hideKeyboard()
        setNavigationResult(requestKey, bundleOf(
                coinResultKey to coin,
                selectTypeResultKey to selectType
        ))
        Handler().postDelayed({
            findNavController().popBackStack()
        }, 100)
    }

    companion object {

        const val requestKey = "selectSwapCoinRequestKey"
        const val coinResultKey = "coinResultKey"
        const val selectTypeResultKey = "selectTypeResultKey"

        const val EXCLUDED_COIN_KEY = "excludedCoinKey"
        const val HIDE_ZERO_BALANCE_KEY = "hideZeroBalanceKey"
        const val SELECT_TYPE_KEY = "selectTypeKey"

        fun params(selectType: SwapFragment.SelectType, hideZeroBalance: Boolean, excludedCoin: Coin?): Bundle {
            return Bundle(3).apply {
                putParcelable(EXCLUDED_COIN_KEY, excludedCoin)
                putBoolean(HIDE_ZERO_BALANCE_KEY, hideZeroBalance)
                putParcelable(SELECT_TYPE_KEY, selectType)
            }
        }
    }
}
