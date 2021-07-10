package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchDialogFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.CoinBalanceItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult

class SelectSwapCoinDialogFragment : BaseWithSearchDialogFragment() {

    private var viewModel: SelectSwapCoinViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.window?.setWindowAnimations(R.style.RightDialogAnimations)
        return inflater.inflate(R.layout.fragment_swap_select_token, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        configureSearchMenu(toolbar.menu)

        val coinBalanceItems = arguments?.getParcelableArrayList<CoinBalanceItem>(coinBalanceItemsListKey)
        val requestId = arguments?.getInt(requestIdKey)
        if (coinBalanceItems == null || requestId == null) {
            findNavController().popBackStack()
            return
        }

        viewModel = ViewModelProvider(this, SelectSwapCoinModule.Factory(coinBalanceItems))
                .get(SelectSwapCoinViewModel::class.java)

        val adapter = SelectSwapCoinAdapter(onClickItem = { closeWithResult(it, requestId) })

        view.findViewById<RecyclerView>(R.id.recyclerView).adapter = adapter

        viewModel?.coinItemsLivedData?.observe(viewLifecycleOwner, { items ->
            adapter.items = items
            adapter.notifyDataSetChanged()
        })

    }

    override fun updateFilter(query: String) {
        viewModel?.updateFilter(query)
    }

    private fun closeWithResult(coinBalanceItem: CoinBalanceItem, requestId: Int) {
        setNavigationResult(resultBundleKey, bundleOf(
                requestIdKey to requestId,
                coinBalanceItemResultKey to coinBalanceItem
        ))
        Handler(Looper.getMainLooper()).postDelayed({
            findNavController().popBackStack()
        }, 100)
    }

    companion object {
        const val resultBundleKey = "selectSwapCoinResultKey"
        const val coinBalanceItemsListKey = "coinBalanceItemsListKey"
        const val requestIdKey = "requestIdKey"
        const val coinBalanceItemResultKey = "coinBalanceItemResultKey"

        fun params(requestId: Int, coinBalanceItems: ArrayList<CoinBalanceItem>): Bundle {
            return bundleOf(
                    requestIdKey to requestId,
                    coinBalanceItemsListKey to coinBalanceItems
            )
        }
    }

}
