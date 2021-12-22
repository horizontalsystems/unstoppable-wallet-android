package io.horizontalsystems.bankwallet.modules.showkey.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentShowWordsTabBinding

class ShowWordsTab : BaseFragment() {
    private val words: List<String>
        get() = requireArguments().getStringArrayList(WORDS) ?: listOf()

    private val passphrase: String
        get() = requireArguments().getString(PASSPHRASE) ?: ""

    private var _binding: FragmentShowWordsTabBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowWordsTabBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mnemonicPhraseView.populateWords(words, passphrase)
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
