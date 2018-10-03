package bitcoin.wallet.ui.view

import android.support.v7.widget.RecyclerView
import android.view.View

import bitcoin.wallet.R


class ViewHolderProgressbar(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(visible: Boolean) {
        itemView.findViewById<View>(R.id.progressBar).visibility = if (visible) View.VISIBLE else View.GONE
    }

    companion object {
        val layoutResourceId: Int
            get() = R.layout.view_holder_progressbar_item
    }
}
