package io.horizontalsystems.bankwallet.modules.backup

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.lib.WordsAdapter
import kotlinx.android.synthetic.main.fragment_backup_words_show_words.*

class BackupWordsFragment : Fragment() {

    private lateinit var viewModel: BackupViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_show_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val wordsAdapter = WordsAdapter()

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(BackupViewModel::class.java)
        }

        recyclerWords.adapter = wordsAdapter
        recyclerWords.layoutManager = LinearLayoutManager(context)

        viewModel.wordsLiveData.observe(this, Observer {
            it?.let {
                wordsAdapter.items = it
                wordsAdapter.notifyDataSetChanged()
            }
        })
    }

}
