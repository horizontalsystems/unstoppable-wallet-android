package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import io.horizontalsystems.bankwallet.databinding.ViewMnemonicPhraseBinding
import io.horizontalsystems.views.BackupWordView

class MnemonicPhraseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    private val binding = ViewMnemonicPhraseBinding.inflate(LayoutInflater.from(context), this)

    fun populateWords(words: List<String>, passphrase: String) {
        val is12Words = words.count() == 12

        words.forEachIndexed { index, word ->
            val normalizedIndex = index + 1
            val wordView = BackupWordView(context).apply {
                bind("$normalizedIndex.", word)
            }
            val viewGroup = if (is12Words) {
                when {
                    normalizedIndex <= 3 -> binding.topLeft
                    normalizedIndex <= 6 -> binding.middleLeft
                    normalizedIndex <= 9 -> binding.topRight
                    else -> binding.middleRight
                }
            } else {
                when {
                    normalizedIndex <= 4 -> binding.topLeft
                    normalizedIndex <= 8 -> binding.middleLeft
                    normalizedIndex <= 12 -> binding.bottomLeft
                    normalizedIndex <= 16 -> binding.topRight
                    normalizedIndex <= 20 -> binding.middleRight
                    else -> binding.bottomRight
                }
            }
            viewGroup.addView(wordView)
        }

        binding.passphraseGroup.isVisible = passphrase.isNotBlank()
        binding.passphraseValue.text = passphrase
    }

}
