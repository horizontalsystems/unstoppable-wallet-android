package io.horizontalsystems.bankwallet.modules.backup.words

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_backup_words_list.*

class BackupWordsListFragment : BaseFragment() {

    val viewModel by activityViewModels<BackupWordsViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(false)

        viewModel.wordsLiveData.observe(viewLifecycleOwner, Observer {
            populateWords(it)
        })

        viewModel.backedUpLiveData.observe(viewLifecycleOwner, Observer { backedUp ->
            buttonClose.isVisible = backedUp
            buttonNext.isVisible = !backedUp
        })

        buttonNext.setOnSingleClickListener {
            viewModel.delegate.onNextClick()
        }

        buttonClose.setOnSingleClickListener {
            viewModel.delegate.onCloseClick()
        }
    }

    private fun populateWords(words: Array<String>) {
        context?.let { ctx ->
            val numberColor = ContextCompat.getColor(ctx, R.color.grey)
            val wordColor = LayoutHelper.getAttr(R.attr.ColorOz, ctx.theme)
                    ?: ContextCompat.getColor(ctx, R.color.grey)
            val sb = SpannableStringBuilder()

            words.forEachIndexed { index, word ->
                val normalizedIndex = index + 1
                val wordString = "$word\n"
                var numberString = "$normalizedIndex. "
                if (normalizedIndex < 10) {
                    numberString += "  " //add two spaces to to make equal alignment for all words
                }

                val numberSpan = SpannableString(numberString)
                numberSpan.setSpan(ForegroundColorSpan(numberColor), 0, numberString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                val wordSpan = SpannableString(wordString)
                wordSpan.setSpan(ForegroundColorSpan(wordColor), 0, wordString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                sb.append(numberSpan)
                sb.append(wordSpan)

                when (normalizedIndex) {
                    6 -> {
                        topLeft.text = sb
                        sb.clear()
                    }
                    12 -> {
                        topRight.text = sb
                        sb.clear()
                    }
                    18 -> {
                        bottomLeft.text = sb
                        sb.clear()
                    }
                    24 -> {
                        bottomRight.text = sb
                        sb.clear()
                    }
                }
            }
        }
    }
}
