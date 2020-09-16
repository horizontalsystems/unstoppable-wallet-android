package io.horizontalsystems.bankwallet.modules.backup.words

import android.os.Bundle
import android.view.*
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_backup_words_confirm.*

class BackupWordsConfirmFragment : BaseFragment() {

    val viewModel by activityViewModels<BackupWordsViewModel>()

    private var wordIndex1 = -1
    private var wordIndex2 = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        textDescription.text = getString(R.string.Backup_Confirmation_Description, getString(viewModel.accountTypeTitle))

        viewModel.wordIndexesToConfirmLiveData.observe(viewLifecycleOwner, Observer { list ->
            list?.let {
                wordOne?.bindPrefix("${it[0]}.")
                wordTwo?.bindPrefix("${it[1]}.")

                wordIndex1 = it[0]
                wordIndex2 = it[1]
            }
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            HudHelper.showErrorMessage(this.requireView(), it)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.backup_words_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.itemDone -> {
                validateWords()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun validateWords() {
        val wordOneEntry = wordOne?.getEnteredText()?.toLowerCase()
        val wordTwoEntry = wordTwo?.getEnteredText()?.toLowerCase()
        if (wordOneEntry.isNullOrEmpty() || wordTwoEntry.isNullOrEmpty()) {
            HudHelper.showErrorMessage(this.requireView(), getString(R.string.Backup_Confirmation_Description, getString(viewModel.accountTypeTitle)))
        } else {
            viewModel.delegate.validateDidClick(hashMapOf(wordIndex1 to wordOneEntry, wordIndex2 to wordTwoEntry))
        }
    }
}
