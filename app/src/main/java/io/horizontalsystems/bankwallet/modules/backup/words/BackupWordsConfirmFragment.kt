package io.horizontalsystems.bankwallet.modules.backup.words

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.InputTextView

class BackupWordsConfirmFragment : Fragment() {

    val viewModel by activityViewModels<BackupWordsViewModel>()

    private var wordIndex1 = -1
    private var wordIndex2 = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val wordOne: InputTextView? = view.findViewById(R.id.wordOne)
        val wordTwo: InputTextView? = view.findViewById(R.id.wordTwo)

        viewModel.wordIndexesToConfirmLiveData.observe(viewLifecycleOwner, Observer { list ->
            list?.let {
                wordOne?.bindPrefix("${it[0]}.")
                wordTwo?.bindPrefix("${it[1]}.")

                wordIndex1 = it[0]
                wordIndex2 = it[1]
            }
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            context?.let { context -> HudHelper.showErrorMessage(context, it) }
        })

        viewModel.validateWordsLiveEvent.observe(viewLifecycleOwner, Observer {
            val wordOneEntry = wordOne?.getEnteredText()?.toLowerCase()
            val wordTwoEntry = wordTwo?.getEnteredText()?.toLowerCase()
            if (wordOneEntry.isNullOrEmpty() || wordTwoEntry.isNullOrEmpty()) {
                context?.let { context -> HudHelper.showErrorMessage(context, getString(R.string.Backup_Confirmation_Description)) }
            } else {
                viewModel.delegate.validateDidClick(
                        hashMapOf(wordIndex1 to wordOneEntry, wordIndex2 to wordTwoEntry)
                )
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.backup_words_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.itemDone ->  {
                viewModel.delegate.onNextClick()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

}
