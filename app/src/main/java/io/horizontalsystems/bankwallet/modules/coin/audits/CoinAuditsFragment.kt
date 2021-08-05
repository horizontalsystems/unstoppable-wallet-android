package io.horizontalsystems.bankwallet.modules.coin.audits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinInfoErrorAdapter
import io.horizontalsystems.bankwallet.modules.coin.tvlrank.TvlRankLoadingAdapter
import io.horizontalsystems.bankwallet.modules.market.overview.PoweredByAdapter
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_recyclerview.*

class CoinAuditsFragment : BaseFragment(), CoinAuditsAdapter.Listener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.CoinPage_Audits)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val coinType = arguments?.getParcelable<CoinType>("coinType") ?: run {
            findNavController().popBackStack();
            return
        }

        val viewModel by viewModels<CoinAuditsViewModel> { CoinAuditsModule.Factory(coinType) }

        val adapter = CoinAuditsAdapter(this)
        val loadingAdapter = TvlRankLoadingAdapter(
            viewModel.loadingLiveData,
            viewLifecycleOwner
        )
        val errorAdapter = CoinInfoErrorAdapter(viewModel.coinInfoErrorLiveData, viewLifecycleOwner)
        val poweredByAdapter = PoweredByAdapter(viewModel.showPoweredByLiveData, viewLifecycleOwner, getString(R.string.CoinPage_Audits_PoweredBy))

        recyclerView.adapter = ConcatAdapter(loadingAdapter, errorAdapter, adapter, poweredByAdapter)
        recyclerView.itemAnimator = null

        viewModel.coinAudits.observe(viewLifecycleOwner, {
            adapter.items = it
            adapter.notifyDataSetChanged()
        })
    }

    override fun onItemClick(url: String) {
        context?.let { ctx ->
            LinkHelper.openLinkInAppBrowser(ctx, url)
        }
    }
}
