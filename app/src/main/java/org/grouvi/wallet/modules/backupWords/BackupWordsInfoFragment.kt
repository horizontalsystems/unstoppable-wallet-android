package org.grouvi.wallet.modules.backupWords

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_backup_words_info.*
import org.grouvi.wallet.R

class BackupWordsInfoFragment : Fragment() {
    private lateinit var viewModel: BackupWordsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(BackupWordsViewModel::class.java)
        }

        buttonBackup.setOnClickListener {
            Log.e("AAA", "Yahoo, ${viewModel.presenter}")

            viewModel.presenter.showWordsDidTap()
        }

        buttonCancel.setOnClickListener {
            viewModel.presenter.cancelDidTap()
        }
    }

}