package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.BackupWordView

class MnemonicPhraseView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : NestedScrollView(context, attrs, defStyleAttr) {

    private var topLeft: LinearLayout
    private var middleLeft: LinearLayout
    private var topRight: LinearLayout
    private var middleRight: LinearLayout
    private var bottomLeft: LinearLayout
    private var bottomRight: LinearLayout
    private var passphraseGroup: Group
    private var passphraseValue: TextView

    init {
        val rootView = inflate(context, R.layout.view_mnemonic_phrase, this)
        topLeft = rootView.findViewById(R.id.topLeft)
        middleLeft = rootView.findViewById(R.id.middleLeft)
        topRight = rootView.findViewById(R.id.topRight)
        middleRight = rootView.findViewById(R.id.middleRight)
        bottomLeft = rootView.findViewById(R.id.bottomLeft)
        bottomRight = rootView.findViewById(R.id.bottomRight)
        passphraseGroup = rootView.findViewById(R.id.passphraseGroup)
        passphraseValue = rootView.findViewById(R.id.passphraseValue)

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
