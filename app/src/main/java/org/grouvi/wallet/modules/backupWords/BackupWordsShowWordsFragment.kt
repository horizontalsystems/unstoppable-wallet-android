package org.grouvi.wallet.modules.backupWords

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_backup_words_show_words.*
import org.grouvi.wallet.R

class BackupWordsShowWordsFragment : Fragment() {
    private lateinit var viewModel: BackupWordsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_show_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(BackupWordsViewModel::class.java)
        }

        viewModel.wordsLiveData.observe(this, Observer {
            it?.let {
                textWords.text = it.joinToString(", ")
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