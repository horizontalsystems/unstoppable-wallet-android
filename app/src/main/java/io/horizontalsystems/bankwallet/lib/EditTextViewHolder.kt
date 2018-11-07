package io.horizontalsystems.bankwallet.lib

import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class EditTextViewHolder(private val editText: EditText, listener: WordsChangedListener) : RecyclerView.ViewHolder(editText) {

    interface WordsChangedListener {
        fun set(position: Int, value: String)
    }

    init {
        editText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable) {
                listener.set(adapterPosition, s.toString().trim())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

    fun bind(position: Int) {
        editText.hint = "${position + 1}"
    }

}