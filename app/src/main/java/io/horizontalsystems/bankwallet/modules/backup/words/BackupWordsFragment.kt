package io.horizontalsystems.bankwallet.modules.backup.words

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult

class BackupWordsFragment : BaseFragment() {

    val viewModel by activityViewModels<BackupWordsViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backedUp = arguments?.getBoolean(ACCOUNT_BACKEDUP, false) ?: false
        val backupWords = arguments?.getStringArray(WORDS_KEY) ?: arrayOf()
        val typeTitle = arguments?.getInt(ACCOUNT_TYPE_TITLE, R.string.AccountType_Unstoppable) ?: 0
        val additionalInfo = arguments?.getString(ACCOUNT_ADDITIONAL_INFO)

        viewModel.accountTypeTitle = typeTitle
        viewModel.init(backupWords, backedUp, additionalInfo)

        if (savedInstanceState == null) {
            viewModel.delegate.viewDidLoad()
        }

        observeEvents()
    }

    private fun observeEvents() {
        viewModel.loadPageLiveEvent.observe(viewLifecycleOwner, Observer { page ->
            val fragment = when (page) {
                1 -> BackupWordsListFragment()
                else -> BackupWordsConfirmFragment()
            }

            childFragmentManager.beginTransaction().apply {
                replace(R.id.fragmentContainer, fragment)
                commit()
            }
        })

        viewModel.notifyBackedUpEvent.observe(viewLifecycleOwner, Observer {
            setNavigationResult(BackupWordsModule.requestKey, bundleOf(
                    BackupWordsModule.requestResult to BackupWordsModule.RESULT_BACKUP
            ))
            findNavController().popBackStack()
        })

        viewModel.notifyClosedEvent.observe(viewLifecycleOwner, Observer {
            setNavigationResult(BackupWordsModule.requestKey, bundleOf(
                    BackupWordsModule.requestResult to BackupWordsModule.RESULT_SHOW
            ))
            findNavController().popBackStack()
        })

        viewModel.closeLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().popBackStack()
        })
    }

    companion object {
        const val ACCOUNT_BACKEDUP = "account_backedup"
        const val WORDS_KEY = "words"
        const val ACCOUNT_TYPE_TITLE = "account_type_title"
        const val ACCOUNT_ADDITIONAL_INFO = "account_additional_info"
    }
}
