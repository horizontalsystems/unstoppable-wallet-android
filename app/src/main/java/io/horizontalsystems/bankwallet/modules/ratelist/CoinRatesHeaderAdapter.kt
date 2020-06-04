package io.horizontalsystems.bankwallet.modules.ratelist

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_date.*
import kotlinx.android.synthetic.main.view_holder_title.*
import java.util.*

class CoinRatesHeaderAdapter(private val title: CharSequence, private val sortButtonClickListener: View.OnClickListener? = null) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var timestamp = 0L
        set(value) {
            field = value

            notifyItemChanged(0)
        }

    override fun getItemCount() = 2

    override fun getItemViewType(position: Int): Int {
        return when(position) {
            0 -> DateViewHolder.layout
            1 -> TitleViewHolder.layout
            else -> throw IllegalStateException()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            DateViewHolder.layout -> DateViewHolder.create(parent)
            TitleViewHolder.layout -> TitleViewHolder.create(parent, sortButtonClickListener)
            else -> throw IllegalStateException("Undefined viewType: $viewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DateViewHolder -> holder.setTimestamp(timestamp)
            is TitleViewHolder -> holder.setTitle(title)
        }
    }

    class DateViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun setTimestamp(timestamp: Long) {
            date.text = DateHelper.getDayAndTime(Date(timestamp * 1000))
        }

        companion object {
            const val layout = R.layout.view_holder_date

            fun create(parent: ViewGroup) = DateViewHolder(inflate(parent, layout, false))
        }

    }

    class TitleViewHolder(override val containerView: View, sortButtonClickListener: View.OnClickListener?) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        init {
            sortButton.isVisible = sortButtonClickListener != null
            sortButton.setOnClickListener(sortButtonClickListener)
        }

        fun setTitle(titleStr: CharSequence) {
            title.text = titleStr
        }

        companion object {
            const val layout = R.layout.view_holder_title

            fun create(parent: ViewGroup, sortButtonClickListener: View.OnClickListener?): TitleViewHolder {
                return TitleViewHolder(inflate(parent, layout, false), sortButtonClickListener)
            }
        }

    }


}


