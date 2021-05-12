package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.BackupWordView
import kotlinx.android.synthetic.main.view_mnemonic_phrase.view.*

class MnemonicPhraseView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : NestedScrollView(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_mnemonic_phrase, this)
    }

    fun populateWords(words: List<String>, passphrase: String) {
        val is12Words = words.count() == 12

        words.forEachIndexed { index, word ->
            val normalizedIndex = index + 1
            val wordView = BackupWordView(context).apply {
                bind("$normalizedIndex.", word)
            }
            val viewGroup = if (is12Words) {
                when {
                    normalizedIndex <= 3 -> topLeft
                    normalizedIndex <= 6 -> middleLeft
                    normalizedIndex <= 9 -> topRight
                    else -> middleRight
                }
            } else {
                when {
                    normalizedIndex <= 4 -> topLeft
                    normalizedIndex <= 8 -> middleLeft
                    normalizedIndex <= 12 -> bottomLeft
                    normalizedIndex <= 16 -> topRight
                    normalizedIndex <= 20 -> middleRight
                    else -> bottomRight
                }
            }
            viewGroup.addView(wordView)
        }

        passphraseGroup.isVisible = passphrase.isNotBlank()
        passphraseValue.text = passphrase
    }

}
