package io.horizontalsystems.bankwallet.modules.backup.words

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.BackupWordView
import kotlinx.android.synthetic.main.fragment_backup_words_list.*

class BackupWordsListFragment : BaseFragment() {

    val viewModel by activityViewModels<BackupWordsViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.wordsLiveData.observe(viewLifecycleOwner, Observer {
            populateWords(it)
        })

        viewModel.backedUpLiveData.observe(viewLifecycleOwner, Observer { backedUp ->
            buttonClose.isVisible = backedUp
            buttonNext.isVisible = !backedUp
        })

        viewModel.additionalInfoLiveData.observe(viewLifecycleOwner, { additionalInfo ->
            additionalInfoLayout.isVisible = additionalInfo != null
            additionalInfoButton.text = additionalInfo
        })

        buttonNext.setOnSingleClickListener {
            viewModel.delegate.onNextClick()
        }

        buttonClose.setOnSingleClickListener {
            viewModel.delegate.onCloseClick()
        }

        additionalInfoButton.setOnClickListener {
            TextHelper.copyText(additionalInfoButton.text.toString())
            HudHelper.showSuccessMessage(this.requireView(), R.string.Hud_Text_Copied)
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
