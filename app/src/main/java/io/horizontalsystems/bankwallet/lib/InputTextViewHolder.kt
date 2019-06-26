package io.horizontalsystems.bankwallet.lib

import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.ui.extensions.InputTextView

class InputTextViewHolder(private val inputTextView: InputTextView, private val listener: WordsChangedListener)
    : RecyclerView.ViewHolder(inputTextView) {

    interface WordsChangedListener {
        fun set(position: Int, value: String)
        fun done()
    }

    init {
        inputTextView.bindTextChangeListener { listener.set(adapterPosition, it) }
    }

    fun bind(position: Int, lastElement: Boolean) {
        inputTextView.bindPrefix("${position + 1}")
        if (lastElement) {
            inputTextView.setImeActionDone { listener.done() }
        }
    }

}
