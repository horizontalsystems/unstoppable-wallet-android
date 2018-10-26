package bitcoin.wallet.modules.backup

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bitcoin.wallet.R
import kotlinx.android.synthetic.main.fragment_backup_words_info.*

class BackupInfoFragment : Fragment() {
    private lateinit var viewModel: BackupViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(BackupViewModel::class.java)
        }

        buttonBackup.setOnClickListener { viewModel.delegate.showWordsDidClick() }

        buttonLater.setOnClickListener { viewModel.delegate.laterDidClick() }
    }

}

