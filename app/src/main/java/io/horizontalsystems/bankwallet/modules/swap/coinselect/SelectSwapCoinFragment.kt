package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.view.SwapFragment
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
            it.supportActionBar?.title  = getString(R.string.ManageCoins_title)
        }

        val excludedCoin = arguments?.getParcelable<Coin>("excludedCoinKey")
        val hideZeroBalance = arguments?.getBoolean("hideZeroBalanceKey")
        val selectType = arguments?.getParcelable<SwapFragment.SelectType>("selectTypeKey") ?: run{
            activity?.supportFragmentManager?.popBackStack()
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
                activity?.supportFragmentManager?.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun updateFilter(query: String) {
        viewModel?.updateFilter(query)
    }

    private fun closeWithResult(coin: Coin, selectType: SwapFragment.SelectType) {
        setFragmentResult(requestKey, bundleOf(
                coinResultKey to coin,
                selectTypeResultKey to selectType
        ))
        activity?.supportFragmentManager?.popBackStack()
    }

    companion object {

        const val requestKey = "selectSwapCoinRequestKey"
        const val coinResultKey = "coinResultKey"
        const val selectTypeResultKey = "selectTypeResultKey"

        fun start(activity: FragmentActivity?, selectType: SwapFragment.SelectType, hideZeroBalance: Boolean, excludedCoin: Coin?) {
            activity?.supportFragmentManager?.commit {
                add(R.id.fragmentContainerView, instance(selectType, hideZeroBalance, excludedCoin))
                addToBackStack(null)
            }
        }

        fun instance(selectType: SwapFragment.SelectType,  hideZeroBalance: Boolean, excludedCoin: Coin?): SelectSwapCoinFragment {
            return SelectSwapCoinFragment().apply {
                arguments = Bundle(3).apply {
                    putParcelable("excludedCoinKey", excludedCoin)
                    putBoolean("hideZeroBalanceKey", hideZeroBalance)
                    putParcelable("selectTypeKey", selectType)
                }
            }
        }
    }

}
