package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.BackupWordView
import kotlinx.android.synthetic.main.view_mnemonic_phrase.view.*

class MnemonicPhraseView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : NestedScrollView(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_mnemonic_phrase, this)
    }

    fun populateWords(words: List<String>) {
        words.forEachIndexed { index, word ->
            val normalizedIndex = index + 1
            val wordView = BackupWordView(context).apply {
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
