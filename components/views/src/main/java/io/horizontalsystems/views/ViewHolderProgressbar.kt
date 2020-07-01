package io.horizontalsystems.views

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class ViewHolderProgressbar(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(visible: Boolean) {
        itemView.findViewById<View>(R.id.progressBar).isVisible = visible
    }

    companion object {
        val layoutResourceId: Int
            get() = R.layout.view_holder_progressbar_item
    }
}
