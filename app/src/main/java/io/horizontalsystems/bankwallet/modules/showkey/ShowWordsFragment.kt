package io.horizontalsystems.bankwallet.modules.showkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.BackupWordView
import kotlinx.android.synthetic.main.fragment_show_words.*

open class ShowWordsFragment : BaseFragment() {
    private val words: Array<String>
        get() = requireArguments().getStringArray(WORDS) ?: arrayOf()

    @StringRes
    protected open val actionButtonText: Int = R.string.Button_Close

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        actionButton.setText(actionButtonText)
        actionButton.setOnSingleClickListener {
            onActionButtonClick()
        }

        populateWords(words)
    }

    protected open fun onActionButtonClick() {
        findNavController().popBackStack(R.id.showKeyFragment, true)
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

    companion object {
        const val WORDS = "words"
    }

}
