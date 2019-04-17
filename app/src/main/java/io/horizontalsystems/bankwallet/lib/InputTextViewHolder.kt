package io.horizontalsystems.bankwallet.lib

import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.ui.extensions.InputTextView

class InputTextViewHolder(private val inputTextView: InputTextView, listener: WordsChangedListener)
    : RecyclerView.ViewHolder(inputTextView) {

    interface WordsChangedListener {
        fun set(position: Int, value: String)
    }

    init {
        inputTextView.bindTextChangeListener { listener.set(adapterPosition, it) }
    }

    fun bind(position: Int) {
        inputTextView.bindPrefix("${position + 1}")
    }

}
