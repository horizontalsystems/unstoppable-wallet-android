package io.horizontalsystems.bankwallet.lib

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import io.horizontalsystems.bankwallet.R

class WordsInputAdapter(private val listener: EditTextViewHolder.WordsChangedListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount() = 12

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditTextViewHolder {
        val editText = LayoutInflater.from(parent.context).inflate(R.layout.view_holder_input_word, parent, false) as EditText
        return EditTextViewHolder(editText, listener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? EditTextViewHolder)?.bind(position)
    }

}