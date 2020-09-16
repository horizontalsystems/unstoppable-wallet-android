package io.horizontalsystems.bankwallet.modules.backup.words

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import kotlinx.android.synthetic.main.fragment_backup_words.*

class BackupWordsFragment : BaseFragment() {

    val viewModel by activityViewModels<BackupWordsViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(false)

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        val backedUp = arguments?.getBoolean(ACCOUNT_BACKEDUP, false) ?: false
        val backupWords = arguments?.getStringArray(WORDS_KEY) ?: arrayOf()
        val typeTitle = arguments?.getInt(ACCOUNT_TYPE_TITLE, R.string.AccountType_Unstoppable) ?: 0

        viewModel.accountTypeTitle = typeTitle
        viewModel.init(backupWords, backedUp)

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
                addToBackStack(null)
                commit()
            }

            collapsingToolbar.title = when (page) {
                1 -> getString(R.string.Backup_DisplayTitle)
                2 -> getString(R.string.Backup_Confirmation_CheckTitle)
                else -> null
            }
        })

        viewModel.notifyBackedUpEvent.observe(viewLifecycleOwner, Observer {
            setFragmentResult(BackupWordsModule.requestKey, bundleOf(
                    BackupWordsModule.requestResult to BackupWordsModule.RESULT_BACKUP
            ))
            activity?.supportFragmentManager?.popBackStack()
        })

        viewModel.notifyClosedEvent.observe(viewLifecycleOwner, Observer {
            setFragmentResult(BackupWordsModule.requestKey, bundleOf(
                    BackupWordsModule.requestResult to BackupWordsModule.RESULT_SHOW
            ))
            activity?.supportFragmentManager?.popBackStack()
        })

        viewModel.closeLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.supportFragmentManager?.popBackStack()
        })
    }

    companion object {
        const val ACCOUNT_BACKEDUP = "account_backedup"
        const val WORDS_KEY = "words"
        const val ACCOUNT_TYPE_TITLE = "account_type_title"

        fun start(activity: FragmentActivity, words: List<String>, backedUp: Boolean, accountTypeTitle: Int) {
            val fragment = BackupWordsFragment().apply {
                arguments = Bundle(3).apply {
                    putStringArray(WORDS_KEY, words.toTypedArray())
                    putBoolean(ACCOUNT_BACKEDUP, backedUp)
                    putInt(ACCOUNT_TYPE_TITLE, accountTypeTitle)
                }
            }

            activity.supportFragmentManager.commit {
                add(R.id.fragmentContainerView, fragment)
                addToBackStack(null)
            }
        }
    }
}
