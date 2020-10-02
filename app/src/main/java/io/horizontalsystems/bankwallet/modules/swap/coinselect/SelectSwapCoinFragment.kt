package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.view.SwapFragment
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.fragment_swap_select_token.*

class SelectSwapCoinFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap_select_token, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val excludedCoin = arguments?.getParcelable<Coin>("excludedCoinKey")
        val hideZeroBalance = arguments?.getBoolean("hideZeroBalance")
        val selectType = arguments?.getParcelable<SwapFragment.SelectType>("selectTypeKey") ?: run{
            activity?.supportFragmentManager?.popBackStack()
            return
        }

        val viewModel by viewModels<SelectSwapCoinViewModel> { SelectSwapCoinModule.Factory(excludedCoin, hideZeroBalance) }

        val adapter = SelectSwapCoinAdapter(onClickItem = { closeWithResult(it.coin, selectType) })

        shadowlessToolbar.bind(
                getString(R.string.ManageCoins_title),
                rightBtnItem = TopMenuItem(text = R.string.Button_Close, onClick = {
                    activity?.supportFragmentManager?.popBackStack()
                })
        )

        searchView.bind(
                hint = getString(R.string.ManageCoins_Search),
                onTextChanged = { query ->
                    viewModel.updateFilter(query)
                })

        recyclerView.adapter = adapter

        viewModel.coinItemsLivedData.observe(viewLifecycleOwner, Observer { items ->
            adapter.items = items
            adapter.notifyDataSetChanged()
        })

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
