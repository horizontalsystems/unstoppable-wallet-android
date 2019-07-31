package io.horizontalsystems.bankwallet.modules.backup.words

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.inflate

class BackupWordsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items: Array<String> = arrayOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderWord(inflate(parent, R.layout.view_holder_word) as TextView)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderWord -> holder.textView.text = "${position + 1}. ${items[position]}"
        }
    }

    class ViewHolderWord(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
