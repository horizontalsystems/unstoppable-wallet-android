package io.horizontalsystems.bankwallet.modules.backup

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Utils
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.fragment_backup_words_confirm.*

class BackupConfirmFragment : Fragment() {
    private lateinit var viewModel: BackupViewModel

    private var wordIndex1 = -1
    private var wordIndex2 = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(BackupViewModel::class.java)
        }

        context?.let {
            if (Utils.isUsingCustomKeyboard(it) ) {
                (activity as? BaseActivity)?.showCustomKeyboardAlert()
            }
        }

        viewModel.wordIndexesToConfirmLiveData.observe(this, Observer { list ->
            list?.let {
                textWordNumber1.text = "${it[0]}."
                textWordNumber2.text = "${it[1]}."

                wordIndex1 = it[0]
                wordIndex2 = it[1]
            }
        })

        viewModel.errorLiveData.observe(this, Observer {
            showError(it)
        })

        viewModel.validateWordsLiveEvent.observe(this, Observer {
            if (editWord1.text?.isEmpty() == true || editWord2.text?.isEmpty() == true) {
                showError(R.string.Backup_Confirmation_Description)
            } else {
                viewModel.delegate.validateDidClick(
                        hashMapOf(wordIndex1 to editWord1.text.toString(),
                                wordIndex2 to editWord2.text.toString())
                )
            }
        })
    }

    private fun showError(errorMsgId: Int?) {
        errorMsgId?.let { HudHelper.showErrorMessage(it) }
    }

}
