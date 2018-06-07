package org.grouvi.wallet.modules.backupWords

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_backup_words_show_words.*
import org.grouvi.wallet.R
import org.grouvi.wallet.lib.WordsAdapter

class BackupWordsShowWordsFragment : Fragment() {

    private lateinit var viewModel: BackupWordsViewModel
    private val wordsAdapter = WordsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_show_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(BackupWordsViewModel::class.java)
        }

        recyclerWords.adapter = wordsAdapter
        recyclerWords.layoutManager = LinearLayoutManager(context)

        viewModel.wordsLiveData.observe(this, Observer {
            it?.let {
                wordsAdapter.items = it
                wordsAdapter.notifyDataSetChanged()
            }
        })

        buttonBack.setOnClickListener {
            viewModel.presenter.hideWordsDidTap()
        }

        buttonNext.setOnClickListener {
            viewModel.presenter.showConfirmationDidTap()
        }

    }


}