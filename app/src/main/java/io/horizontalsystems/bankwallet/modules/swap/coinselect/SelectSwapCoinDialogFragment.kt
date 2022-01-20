package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchDialogFragment
import io.horizontalsystems.bankwallet.databinding.FragmentSwapSelectTokenBinding
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.CoinBalanceItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult

class SelectSwapCoinDialogFragment : BaseWithSearchDialogFragment() {

    private var viewModel: SelectSwapCoinViewModel? = null

    private var _binding: FragmentSwapSelectTokenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwapSelectTokenBinding.inflate(inflater, container, false)
        val view = binding.root
        dialog?.window?.setWindowAnimations(R.style.RightDialogAnimations)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        configureSearchMenu(binding.toolbar.menu)

        val dex = arguments?.getParcelable<SwapMainModule.Dex>(dexKey)
        val requestId = arguments?.getLong(requestIdKey)
        if (dex == null || requestId == null) {
            findNavController().popBackStack()
            return
        }

        viewModel = ViewModelProvider(this, SelectSwapCoinModule.Factory(dex))
            .get(SelectSwapCoinViewModel::class.java)

        val adapter = SelectSwapCoinAdapter(onClickItem = { closeWithResult(it, requestId) })

        binding.recyclerView.adapter = adapter

        viewModel?.coinItemsLivedData?.observe(viewLifecycleOwner, { items ->
            adapter.items = items
            adapter.notifyDataSetChanged()
        })

    }

    override fun updateFilter(query: String) {
        viewModel?.updateFilter(query)
    }

    private fun closeWithResult(coinBalanceItem: CoinBalanceItem, requestId: Long) {
        setNavigationResult(
            resultBundleKey, bundleOf(
                requestIdKey to requestId,
                coinBalanceItemResultKey to coinBalanceItem
            )
        )
        Handler(Looper.getMainLooper()).postDelayed({
            findNavController().popBackStack()
        }, 100)
    }

    companion object {
        const val resultBundleKey = "selectSwapCoinResultKey"
        const val dexKey = "dexKey"
        const val requestIdKey = "requestIdKey"
        const val coinBalanceItemResultKey = "coinBalanceItemResultKey"

        fun prepareParams(requestId: Long, dex: SwapMainModule.Dex) = bundleOf(requestIdKey to requestId, dexKey to dex)
    }

}
