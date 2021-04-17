package io.horizontalsystems.bankwallet.modules.showkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.backup.words.BackupWordsModule
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult
import io.horizontalsystems.views.BackupWordView
import kotlinx.android.synthetic.main.fragment_backup_words_list.*

class ShowKeyWordsFragment : BaseFragment() {
    private val words: Array<String>
        get() = requireArguments().getStringArray(ShowKeyModule.WORDS) ?: arrayOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_key_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        populateWords(words)

        buttonClose.setOnSingleClickListener {
            setNavigationResult(BackupWordsModule.requestKey, bundleOf(
                    ShowKeyModule.SHOW_KEY_REQUEST to ShowKeyModule.RESULT_OK
            ))
            findNavController().popBackStack()
        }

    }

    private fun populateWords(words: Array<String>) {
        context?.let { ctx ->
            words.forEachIndexed { index, word ->
                val normalizedIndex = index + 1
                val wordView = BackupWordView(ctx).apply {
                    bind("$normalizedIndex.", word)
                }
                when {
                    normalizedIndex <= 6 -> topLeft.addView(wordView)
                    normalizedIndex <= 12 -> topRight.addView(wordView)
                    normalizedIndex <= 18 -> bottomLeft.addView(wordView)
                    normalizedIndex <= 24 -> bottomRight.addView(wordView)
                }
            }
        }
    }

}
