package io.horizontalsystems.bankwallet.modules.guides

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate

class GuidesAdapter(private var listener: Listener) : RecyclerView.Adapter<ViewHolderGuide>(), ViewHolderGuide.ClickListener {

    interface Listener {
        fun onItemClick(position: Int)
    }

    var items = listOf<GuideViewItem>()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderGuide {
        return ViewHolderGuide(inflate(parent, R.layout.view_holder_guide_preview), this)
    }

    override fun onBindViewHolder(holder: ViewHolderGuide, position: Int) {
        holder.bind(items[position])
    }

    override fun onClick(position: Int) {
        listener.onItemClick(position)
    }
}
