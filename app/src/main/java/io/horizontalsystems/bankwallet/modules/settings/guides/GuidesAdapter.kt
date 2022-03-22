package io.horizontalsystems.bankwallet.modules.settings.guides

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.databinding.ViewHolderGuidePreviewBinding
import io.horizontalsystems.bankwallet.entities.Guide

class GuidesAdapter(private var listener: Listener) : RecyclerView.Adapter<ViewHolderGuide>(),
    ViewHolderGuide.ClickListener {

    interface Listener {
        fun onItemClick(guide: Guide)
    }

    var items = listOf<Guide>()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderGuide {
        return ViewHolderGuide(
            ViewHolderGuidePreviewBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ), this
        )
    }

    override fun onBindViewHolder(holder: ViewHolderGuide, position: Int) {
        holder.bind(items[position])
    }

    override fun onClick(guide: Guide) {
        listener.onItemClick(guide)
    }
}
