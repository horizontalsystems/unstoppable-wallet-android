package io.horizontalsystems.bankwallet.modules.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.InputTextView
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper

class BackupConfirmFragment : Fragment() {
    private lateinit var viewModel: BackupViewModel

    private var wordIndex1 = -1
    private var wordIndex2 = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val wordOne: InputTextView? = view.findViewById(R.id.wordOne)
        val wordTwo: InputTextView? = view.findViewById(R.id.wordTwo)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(BackupViewModel::class.java)
        }

        viewModel.wordIndexesToConfirmLiveData.observe(this, Observer { list ->
            list?.let {
                wordOne?.bindPrefix("${it[0]}.")
                wordTwo?.bindPrefix("${it[1]}.")

                wordIndex1 = it[0]
                wordIndex2 = it[1]
            }
        })

        viewModel.errorLiveData.observe(this, Observer {
            showError(it)
        })

        viewModel.validateWordsLiveEvent.observe(this, Observer {
            val wordOneEntry = wordOne?.getEnteredText()
            val wordTwoEntry = wordTwo?.getEnteredText()
            if (wordOneEntry.isNullOrEmpty() || wordTwoEntry.isNullOrEmpty()) {
                showError(R.string.Backup_Confirmation_Description)
            } else {
                viewModel.delegate.validateDidClick(
                        hashMapOf(wordIndex1 to wordOneEntry, wordIndex2 to wordTwoEntry)
                )
            }
        })
    }

    private fun showError(errorMsgId: Int?) {
        errorMsgId?.let { HudHelper.showErrorMessage(it) }
    }

}
