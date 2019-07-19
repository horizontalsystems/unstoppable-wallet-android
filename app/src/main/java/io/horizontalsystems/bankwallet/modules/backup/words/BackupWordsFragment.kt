package io.horizontalsystems.bankwallet.modules.backup.words

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.lib.WordsAdapter
import kotlinx.android.synthetic.main.fragment_backup_words_show_words.*

class BackupWordsFragment : Fragment() {

    private lateinit var viewModel: BackupWordsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_show_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val wordsAdapter = WordsAdapter()

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(BackupWordsViewModel::class.java)
        }

        recyclerWords.adapter = wordsAdapter
        recyclerWords.layoutManager = LinearLayoutManager(context)

        viewModel.wordsLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                wordsAdapter.items = it
                wordsAdapter.notifyDataSetChanged()
            }
        })
    }

}
