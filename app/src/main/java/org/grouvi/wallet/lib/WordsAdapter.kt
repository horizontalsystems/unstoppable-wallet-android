package org.grouvi.wallet.lib

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.grouvi.wallet.R

class WordsAdapter(private val buttonClickListener: View.OnClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: List<String> = listOf()

    companion object {
        const val viewTypeWord = 1
        const val viewTypeButton = 2
    }

    override fun getItemViewType(position: Int) = when {
        position < items.size -> viewTypeWord
        else -> viewTypeButton
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == viewTypeWord) {
            ViewHolderWord(inflater.inflate(android.R.layout.simple_list_item_1, parent, false) as TextView)
        } else {
            ViewHolderButton(inflater.inflate(R.layout.view_holder_button, parent, false) as Button, buttonClickListener)
        }
    }

    override fun getItemCount() = items.size + 1

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderWord -> holder.textView.text = "${position + 1}. ${items[position]}"
        }
    }

    class ViewHolderWord(val textView: TextView) : RecyclerView.ViewHolder(textView)
    class ViewHolderButton(button: Button, private val buttonClickListener: View.OnClickListener) : RecyclerView.ViewHolder(button) {
        init {
            button.setOnClickListener(buttonClickListener)
        }
    }

}