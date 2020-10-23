package io.horizontalsystems.bankwallet.modules.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backup.eos.BackupEosFragment
import io.horizontalsystems.bankwallet.modules.backup.eos.BackupEosModule
import io.horizontalsystems.bankwallet.modules.backup.words.BackupWordsFragment
import io.horizontalsystems.bankwallet.modules.backup.words.BackupWordsModule
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationLiveData
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.pin.PinInteractionType
import io.horizontalsystems.pin.PinModule
import io.horizontalsystems.snackbar.SnackbarDuration
import kotlinx.android.synthetic.main.fragment_backup.*

class BackupFragment : BaseFragment() {

    private val viewModel by viewModels<BackupViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(false)

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        val accountCoins = arguments?.getString(ModuleField.ACCOUNT_COINS)
        val account = arguments?.getParcelable<Account>(ModuleField.ACCOUNT) ?: run {
            findNavController().popBackStack()
            return
        }

        viewModel.init(account)
        viewModel.startPinModule.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.backupFragment_to_pinFragment, PinModule.forUnlock(), navOptions())
        })

        viewModel.startBackupWordsModule.observe(viewLifecycleOwner, Observer { (words, accountTypeTitle) ->
            val arguments = Bundle(3).apply {
                putStringArray(BackupWordsFragment.WORDS_KEY, words.toTypedArray())
                putBoolean(BackupWordsFragment.ACCOUNT_BACKEDUP, account.isBackedUp)
                putInt(BackupWordsFragment.ACCOUNT_TYPE_TITLE, accountTypeTitle)
            }

            findNavController().navigate(R.id.backupFragment_to_backupWordsFragment, arguments, navOptions())
        })

        viewModel.startBackupEosModule.observe(viewLifecycleOwner, Observer { (account, activePrivateKey) ->
            val arguments = Bundle(2).apply {
                putString(BackupEosFragment.ACCOUNT, account)
                putString(BackupEosFragment.ACTIVE_PRIVATE_KEY, activePrivateKey)
            }

            findNavController().navigate(R.id.backupFragment_to_backupEosFragment, arguments, navOptions())
        })

        viewModel.closeLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().popBackStack()
        })

        viewModel.showSuccessAndFinishEvent.observe(viewLifecycleOwner, Observer {
            activity?.let { HudHelper.showSuccessMessage(it.findViewById(android.R.id.content), R.string.Hud_Text_Done, SnackbarDuration.LONG) }
            findNavController().popBackStack()
        })

        buttonNext.setOnSingleClickListener { viewModel.delegate.onClickBackup() }
        backupIntro.text = getString(R.string.Backup_Intro_Subtitle, accountCoins)

        if (account.isBackedUp) {
            collapsingToolbar.title = getString(R.string.Backup_Intro_TitleShow)
            buttonNext.text = getString(R.string.Backup_Button_ShowKey)
        }

        subscribeFragmentResult()
    }

    private fun subscribeFragmentResult() {
        getNavigationLiveData(PinModule.requestKey)?.observe(viewLifecycleOwner, Observer { bundle ->
            val resultType = bundle.getParcelable<PinInteractionType>(PinModule.requestType)
            val resultCode = bundle.getInt(PinModule.requestResult)

            if (resultType == PinInteractionType.UNLOCK) {
                when (resultCode) {
                    PinModule.RESULT_OK -> viewModel.delegate.didUnlock()
                    PinModule.RESULT_CANCELLED -> viewModel.delegate.didCancelUnlock()
                }
            }
        })

        getNavigationLiveData(BackupWordsModule.requestKey)?.observe(viewLifecycleOwner, Observer { bundle ->
            when (bundle.getInt(BackupWordsModule.requestResult)) {
                BackupWordsModule.RESULT_BACKUP -> viewModel.delegate.didBackup()
                BackupWordsModule.RESULT_SHOW -> {
                    findNavController().popBackStack()
                }
                else -> {
                }
            }
        })

        getNavigationLiveData(BackupEosModule.requestKey)?.observe(viewLifecycleOwner, Observer {
            when (it.getInt(BackupEosModule.requestResult)) {
                BackupEosModule.RESULT_SHOW -> {
                    findNavController().popBackStack()
                }
                else -> {
                }
            }
        })
    }
}
