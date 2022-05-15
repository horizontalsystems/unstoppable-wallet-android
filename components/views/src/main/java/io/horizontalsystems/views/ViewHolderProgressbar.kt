package io.horizontalsystems.views

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.views.databinding.ViewHolderProgressbarItemBinding

class ViewHolderProgressbar(private val binding: ViewHolderProgressbarItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(visible: Boolean) {
        binding.progressBar.isVisible = visible
    }

}
