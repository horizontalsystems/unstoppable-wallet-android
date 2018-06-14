package bitcoin.wallet.modules.backup

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bitcoin.wallet.R
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

        viewModel.wordIndexesToConfirmLiveData.observe(this, Observer {
            it?.let {
                textWordNumber1.text = "${it[0]}."
                textWordNumber2.text = "${it[1]}."

                wordIndex1 = it[0]
                wordIndex2 = it[1]
            }
        })

        viewModel.errorLiveData.observe(this, Observer {
            if (it != null) {
                textError.text = getString(it)
                textError.visibility = View.VISIBLE
            } else {
                textError.visibility = View.GONE
            }
        })

        buttonBack.setOnClickListener {
            viewModel.delegate.hideConfirmationDidClick()
        }

        buttonSubmit.setOnClickListener {
            viewModel.delegate.validateDidClick(
                    hashMapOf(wordIndex1 to editWord1.text.toString(),
                            wordIndex2 to editWord2.text.toString())
            )
        }

    }

}
