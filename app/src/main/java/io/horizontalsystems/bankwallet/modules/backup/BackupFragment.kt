package io.horizontalsystems.bankwallet.modules.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backup.words.BackupWordsFragment
import io.horizontalsystems.bankwallet.modules.backup.words.BackupWordsModule
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
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

        val accountCoins = arguments?.getString(ModuleField.ACCOUNT_COINS)
        val account = arguments?.getParcelable<Account>(ModuleField.ACCOUNT) ?: run {
            findNavController().popBackStack()
            return
        }

        toolbar.title = getString(if (account.isBackedUp) R.string.Backup_Intro_TitleShow else R.string.Backup_Intro_Title)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.init(account)
        viewModel.startPinModule.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.backupFragment_to_pinFragment, PinModule.forUnlock(), navOptions())
        })

        viewModel.startBackupWordsModule.observe(viewLifecycleOwner, { (words, accountTypeTitle, additionalInfo) ->
            val arguments = bundleOf(
                    BackupWordsFragment.WORDS_KEY to words.toTypedArray(),
                    BackupWordsFragment.ACCOUNT_BACKEDUP to account.isBackedUp,
                    BackupWordsFragment.ACCOUNT_TYPE_TITLE to accountTypeTitle,
                    BackupWordsFragment.ACCOUNT_ADDITIONAL_INFO to additionalInfo
            )
            findNavController().navigate(R.id.backupFragment_to_backupWordsFragment, arguments, navOptions())
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
            buttonNext.text = getString(R.string.Backup_Button_ShowKey)
        }

        subscribeFragmentResult()
    }

    private fun subscribeFragmentResult() {
        getNavigationResult(PinModule.requestKey)?.let { bundle ->
            val resultType = bundle.getParcelable<PinInteractionType>(PinModule.requestType)
            val resultCode = bundle.getInt(PinModule.requestResult)

            if (resultType == PinInteractionType.UNLOCK) {
                when (resultCode) {
                    PinModule.RESULT_OK -> viewModel.delegate.didUnlock()
                    PinModule.RESULT_CANCELLED -> viewModel.delegate.didCancelUnlock()
                }
            }
        }

        getNavigationResult(BackupWordsModule.requestKey)?.let { bundle ->
            when (bundle.getInt(BackupWordsModule.requestResult)) {
                BackupWordsModule.RESULT_BACKUP -> viewModel.delegate.didBackup()
                BackupWordsModule.RESULT_SHOW -> {
                    findNavController().popBackStack()
                }
                else -> {
                }
            }
        }
    }
}
