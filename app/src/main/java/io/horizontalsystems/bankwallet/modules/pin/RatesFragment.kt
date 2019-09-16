package io.horizontalsystems.bankwallet.modules.pin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_rates.*
import kotlinx.android.synthetic.main.view_holder_coin_rate.*
import java.util.*

class RatesFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CoinRatesAdapter()
        coinRatesRecyclerView.adapter = adapter


        val today = DateHelper.formatDate(Date(), "MMMM dd")
        dateText.text = today
    }
}


class CoinRatesAdapter : RecyclerView.Adapter<ViewHolderCoinRate>(){

    override fun getItemCount(): Int {
        return 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderCoinRate {
        return ViewHolderCoinRate(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_rate, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderCoinRate, position: Int) {
        holder.bind(position == itemCount - 1)
    }

}

class ViewHolderCoinRate(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(isLast: Boolean) {
        bottomShade.visibility = if (isLast)View.VISIBLE else View.GONE
    }
}