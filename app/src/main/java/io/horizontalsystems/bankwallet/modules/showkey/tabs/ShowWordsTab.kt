package io.horizontalsystems.bankwallet.modules.showkey.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import kotlinx.android.synthetic.main.fragment_show_words_tab.*

class ShowWordsTab : BaseFragment() {
    private val words: List<String>
        get() = requireArguments().getStringArrayList(WORDS) ?: listOf()

    private val passphrase: String
        get() = requireArguments().getString(PASSPHRASE) ?: ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_words_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mnemonicPhraseView.populateWords(words, passphrase)
    }

    companion object {
        private const val WORDS = "words"
        private const val PASSPHRASE = "passphrase"

        fun getInstance(words: List<String>, passphrase: String): ShowWordsTab {
            val fragment = ShowWordsTab()
            val arguments = bundleOf(WORDS to ArrayList(words), PASSPHRASE to passphrase)
            fragment.arguments = arguments
            return fragment
        }
    }

}
