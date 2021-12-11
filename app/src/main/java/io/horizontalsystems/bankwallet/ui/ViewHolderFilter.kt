package io.horizontalsystems.bankwallet.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_filter.*

class ViewHolderFilter(override val containerView: View, private val l: ClickListener) :
    RecyclerView.ViewHolder(containerView), LayoutContainer {

    interface ClickListener {
        fun onClickItem(position: Int, width: Int)
    }

    fun bind(filterId: String?, active: Boolean) {
        buttonFilter.text = filterId
            ?: containerView.context.getString(R.string.Transactions_FilterAll)
        buttonFilter.isActivated = active
        buttonFilter.setOnClickListener {
            l.onClickItem(
                bindingAdapterPosition,
                containerView.width
            )
        }
    }
}