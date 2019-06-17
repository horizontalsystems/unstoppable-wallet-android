package io.horizontalsystems.bankwallet.lib

import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.ui.extensions.InputTextView

class InputTextViewHolder(private val inputTextView: InputTextView, listener: WordsChangedListener, autocompleteAdapter: ArrayAdapter<String>)
    : RecyclerView.ViewHolder(inputTextView) {

    interface WordsChangedListener {
        fun set(position: Int, value: String)
    }

    init {
        inputTextView.setAutocompleteAdapter(autocompleteAdapter)
        inputTextView.bindTextChangeListener { listener.set(adapterPosition, it) }
    }

    fun bind(position: Int) {
        inputTextView.bindPrefix("${position + 1}")
    }

}
