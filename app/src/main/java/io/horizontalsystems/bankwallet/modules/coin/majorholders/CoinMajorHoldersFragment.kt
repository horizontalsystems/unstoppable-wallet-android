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
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.MajorHolderItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_recyclerview.*

class CoinMajorHoldersFragment : BaseFragment(), CoinMajorHoldersAdapter.Listener {

    private val viewModel by viewModels<CoinMajorHoldersViewModel> {
        CoinMajorHoldersModule.Factory(requireArguments().getString(COIN_UID_KEY)!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.CoinPage_MajorHolders)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        pullToRefresh.setProgressBackgroundColorSchemeResource(R.color.claude)
        pullToRefresh.setColorSchemeResources(R.color.oz)

        pullToRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        viewModel.loadingLiveData.observe(viewLifecycleOwner, { loading ->
            pullToRefresh.isRefreshing = loading
        })

        errorViewCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        errorViewCompose.setContent {
            ComposeAppTheme {
                ListErrorView(stringResource(R.string.Market_SyncError)) {
                    viewModel.onErrorClick()
                }
            }
        }

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { viewState ->
            pullToRefresh.isVisible = viewState == ViewState.Success
            errorViewCompose.isVisible = viewState is ViewState.Error
        }

        viewModel.coinMajorHolders.observe(viewLifecycleOwner, { holders ->
            val adapterChart = CoinMajorHoldersPieAdapter(holders.filterIsInstance(MajorHolderItem.Item::class.java))
            val adapterItems = CoinMajorHoldersAdapter(holders, this)
            recyclerView.adapter = ConcatAdapter(adapterChart, adapterItems)
        })
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
