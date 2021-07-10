package io.horizontalsystems.bankwallet.modules.coin.coininvestors

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.core.findNavController

class CoinInvestorsFragment : BaseFragment(), CoinInvestorCategoryAdapter.Listener {

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_coin_investors, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)

        toolbar.title = getString(R.string.CoinPage_FundsInvested)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        coinViewModel.coinInvestors.observe(viewLifecycleOwner, {
            val investorsAdapter = CoinInvestorCategoryAdapter(it, this)
            view.findViewById<RecyclerView>(R.id.recyclerView).adapter = investorsAdapter
        })
    }

    override fun onItemClick(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
