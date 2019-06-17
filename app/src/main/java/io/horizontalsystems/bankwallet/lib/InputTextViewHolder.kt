package io.horizontalsystems.bankwallet.lib

import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.ui.extensions.InputTextView

class InputTextViewHolder(private val inputTextView: InputTextView, private val listener: WordsChangedListener, autocompleteAdapter: ArrayAdapter<String>)
    : RecyclerView.ViewHolder(inputTextView) {

    interface WordsChangedListener {
        fun set(position: Int, value: String)
        fun done()
    }

    init {
        inputTextView.setAutocompleteAdapter(autocompleteAdapter)
        inputTextView.bindTextChangeListener { listener.set(adapterPosition, it) }
    }

    fun bind(position: Int, lastElement: Boolean) {
        inputTextView.bindPrefix("${position + 1}")
        if (lastElement) {
            inputTextView.setImeActionDone { listener.done() }
        } else {
            inputTextView.goToNextWhenItemClicked()
        }
    }

}
