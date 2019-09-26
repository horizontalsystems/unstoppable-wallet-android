package io.horizontalsystems.bankwallet.modules.backup.words

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.synthetic.main.fragment_backup_words.*

class BackupWordsFragment : Fragment() {

    private lateinit var viewModel: BackupWordsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let { viewModel = ViewModelProviders.of(it).get(BackupWordsViewModel::class.java) }

        val wordsAdapter = BackupWordsAdapter()
        recyclerWords.adapter = wordsAdapter
        recyclerWords.layoutManager = LinearLayoutManager(context)

        viewModel.wordsLiveData.observe(viewLifecycleOwner, Observer {
            wordsAdapter.items = it
            wordsAdapter.notifyDataSetChanged()
        })
    }

    class BackupWordsAdapter : RecyclerView.Adapter<ViewHolderWord>() {
        var items: Array<String> = arrayOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderWord {
            return ViewHolderWord(inflate(parent, R.layout.view_holder_word) as TextView)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolderWord, position: Int) {
            holder.textView.text = "${position + 1}. ${items[position]}"
        }
    }

    class ViewHolderWord(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
