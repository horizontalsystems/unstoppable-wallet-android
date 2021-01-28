package io.horizontalsystems.bankwallet.modules.market.discovery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R

class PoweredByAdapter(
        showPoweredBy: LiveData<Boolean>,
        viewLifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<PoweredByAdapter.ViewHolder>() {

    private var show = false

    init {
        showPoweredBy.observe(viewLifecycleOwner, { show ->
            if (this.show != show) {
                this.show = show
                notifyDataSetChanged()
            }
        })
    }

    override fun getItemCount() = if (show) 1 else 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.create(parent)
    }

    class ViewHolder(containerView: View) : RecyclerView.ViewHolder(containerView) {
        companion object {
            fun create(parent: ViewGroup): ViewHolder {
                return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_market_powered_by, parent, false))
            }
        }
    }

}
