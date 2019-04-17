package io.horizontalsystems.bankwallet.lib

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.InputTextView

class WordsInputAdapter(private val listener: InputTextViewHolder.WordsChangedListener) : RecyclerView.Adapter<InputTextViewHolder>() {

    override fun getItemCount() = 12

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InputTextViewHolder {
        val inputTextView = LayoutInflater.from(parent.context).inflate(R.layout.view_holder_input_word, parent, false) as InputTextView
        return InputTextViewHolder(inputTextView, listener)
    }

    override fun onBindViewHolder(holder: InputTextViewHolder, position: Int) {
        holder.bind(position)
    }

}
