package io.horizontalsystems.bankwallet.modules.coin.majorholders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentRecyclerviewBinding
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.MajorHolderItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class CoinMajorHoldersFragment : BaseFragment(), CoinMajorHoldersAdapter.Listener {

    private val viewModel by viewModels<CoinMajorHoldersViewModel> {
        CoinMajorHoldersModule.Factory(requireArguments().getString(COIN_UID_KEY)!!)
    }

    private var _binding: FragmentRecyclerviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecyclerviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = getString(R.string.CoinPage_MajorHolders)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.pullToRefresh.setProgressBackgroundColorSchemeResource(R.color.claude)
        binding.pullToRefresh.setColorSchemeResources(R.color.oz)

        binding.pullToRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        viewModel.isRefreshingLiveData.observe(viewLifecycleOwner) { loading ->
            binding.pullToRefresh.isRefreshing = loading
        }

        binding.errorViewCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        binding.errorViewCompose.setContent {
            ComposeAppTheme {
                ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
            }
        }

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { viewState ->
            binding.pullToRefresh.isVisible = viewState == ViewState.Success
            binding.errorViewCompose.isVisible = viewState is ViewState.Error
        }

        viewModel.coinMajorHolders.observe(viewLifecycleOwner) { holders ->
            val adapterChart = CoinMajorHoldersPieAdapter(holders.filterIsInstance(MajorHolderItem.Item::class.java))
            val adapterItems = CoinMajorHoldersAdapter(holders, this)
            binding.recyclerView.adapter = ConcatAdapter(adapterChart, adapterItems)
        }
    }

    override fun onAddressClick(address: String) {
        TextHelper.copyText(address)
        HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
    }

    override fun onDetailsClick(address: String) {
        context?.let { ctx ->
            LinkHelper.openLinkInAppBrowser(ctx, "https://etherscan.io/address/$address")
        }
    }

    companion object {
        private const val COIN_UID_KEY = "coin_uid_key"

        fun prepareParams(coinUid: String) = bundleOf(COIN_UID_KEY to coinUid)
    }
}
