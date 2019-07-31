package io.horizontalsystems.bankwallet.modules.restore.words

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.InputTextView
import io.horizontalsystems.bankwallet.viewHelpers.inflate

class RestoreWordsAdapter(private val wordsCount: Int, private val listener: Listener)
    : RecyclerView.Adapter<RestoreWordsAdapter.InputTextViewHolder>() {

    interface Listener {
        fun onChange(position: Int, word: String)
        fun onDone()
    }

    override fun getItemCount() = wordsCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InputTextViewHolder {
        return InputTextViewHolder(inflate(parent, R.layout.view_holder_input_word) as InputTextView)
    }

    override fun onBindViewHolder(holder: InputTextViewHolder, position: Int) {
        holder.bind(position, lastElement = position == itemCount - 1)
    }

    inner class InputTextViewHolder(private val inputTextView: InputTextView) : RecyclerView.ViewHolder(inputTextView) {

        init {
            inputTextView.bindTextChangeListener {
                listener.onChange(adapterPosition, it.toLowerCase())
            }
        }

        fun bind(position: Int, lastElement: Boolean) {
            inputTextView.bindPrefix("${position + 1}")

            if (lastElement) {
                inputTextView.setImeActionDone { listener.onDone() }
            }
        }
    }
}
