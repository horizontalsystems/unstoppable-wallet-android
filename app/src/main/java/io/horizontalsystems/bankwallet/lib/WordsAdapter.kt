package io.horizontalsystems.bankwallet.lib

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R

class WordsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items: List<String> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolderWord(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_word, parent, false) as TextView)

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderWord -> holder.textView.text = "${position + 1}. ${items[position]}"
        }
    }

    class ViewHolderWord(val textView: TextView) : RecyclerView.ViewHolder(textView)

}