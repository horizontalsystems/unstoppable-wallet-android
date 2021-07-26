package io.horizontalsystems.bankwallet.modules.coin.audits

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.map
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.modules.market.overview.PoweredByAdapter
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_recyclerview.*

class CoinAuditsFragment : BaseFragment(), CoinAuditsAdapter.Listener {

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.CoinPage_Audits)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        coinViewModel.coinAudits.observe(viewLifecycleOwner, {
            val auditsAdapter = CoinAuditsAdapter(it, this)
            val footerAdapter = PoweredByAdapter(coinViewModel.coinAudits.map { true }, viewLifecycleOwner, getString(R.string.CoinPage_Audits_PoweredBy))

            recyclerView.adapter = ConcatAdapter(auditsAdapter, footerAdapter)
        })
    }

    override fun onItemClick(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
