package io.horizontalsystems.bankwallet.modules.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backup.eos.BackupEosFragment
import io.horizontalsystems.bankwallet.modules.backup.eos.BackupEosModule
import io.horizontalsystems.bankwallet.modules.backup.words.BackupWordsFragment
import io.horizontalsystems.bankwallet.modules.backup.words.BackupWordsModule
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.pin.PinInteractionType
import io.horizontalsystems.pin.PinModule
import kotlinx.android.synthetic.main.fragment_backup.*

class BackupFragment : BaseFragment() {

    private lateinit var viewModel: BackupViewModel

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
            activity?.supportFragmentManager?.popBackStack()
            return
        }

        viewModel = ViewModelProvider(this).get(BackupViewModel::class.java)
        viewModel.init(account)

        buttonNext.setOnSingleClickListener { viewModel.delegate.onClickBackup() }

        viewModel.startPinModule.observe(viewLifecycleOwner, Observer {
            activity?.supportFragmentManager?.commit {
                add(R.id.fragmentContainerView, PinModule.startForUnlock())
                addToBackStack(null)
            }
        })

        viewModel.startBackupWordsModule.observe(viewLifecycleOwner, Observer { (words, accountTypeTitle) ->
            activity?.let {
                BackupWordsFragment.start(it, words, account.isBackedUp, accountTypeTitle)
            }
        })

        viewModel.startBackupEosModule.observe(viewLifecycleOwner, Observer { (account, activePrivateKey) ->
            activity?.let {
                BackupEosFragment.start(it, account, activePrivateKey)
            }
        })

        viewModel.closeLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.supportFragmentManager?.popBackStack()
        })

        viewModel.showSuccessAndFinishEvent.observe(viewLifecycleOwner, Observer {
            activity?.let {
                HudHelper.showSuccessMessage(it.findViewById(android.R.id.content), R.string.Hud_Text_Done, HudHelper.SnackbarDuration.LONG)
                it.supportFragmentManager.popBackStack()
            }
        })

        backupIntro.text = getString(R.string.Backup_Intro_Subtitle, accountCoins)

        if (account.isBackedUp) {
            collapsingToolbar.title = getString(R.string.Backup_Intro_TitleShow)
            buttonNext.text = getString(R.string.Backup_Button_ShowKey)
        }

        subscribeFragmentResult()
    }

    private fun subscribeFragmentResult() {
        val fragmentActivity = activity ?: return

        fragmentActivity.supportFragmentManager.setFragmentResultListener(PinModule.requestKey, viewLifecycleOwner) { _, bundle ->
            val resultType = bundle.getParcelable<PinInteractionType>(PinModule.requestType)
            val resultCode = bundle.getInt(PinModule.requestResult)

            if (resultType == PinInteractionType.UNLOCK) {
                when (resultCode) {
                    PinModule.RESULT_OK -> viewModel.delegate.didUnlock()
                    PinModule.RESULT_CANCELLED -> viewModel.delegate.didCancelUnlock()
                }
            }
        }

        fragmentActivity.supportFragmentManager.setFragmentResultListener(BackupWordsModule.requestKey, viewLifecycleOwner) { _, bundle ->
            when (bundle.getInt(BackupWordsModule.requestResult)) {
                BackupWordsModule.RESULT_BACKUP -> viewModel.delegate.didBackup()
                BackupWordsModule.RESULT_SHOW -> activity?.supportFragmentManager?.popBackStack()
            }
        }

        fragmentActivity.supportFragmentManager.setFragmentResultListener(BackupEosModule.requestKey, viewLifecycleOwner) { _, bundle ->
            when (bundle.getInt(BackupEosModule.requestResult)) {
                BackupEosModule.RESULT_SHOW -> activity?.supportFragmentManager?.popBackStack()
            }
        }
    }

    companion object {
        fun start(activity: FragmentActivity, account: Account, coinCodes: String) {
            val fragment = BackupFragment().apply {
                arguments = Bundle(2).apply {
                    putParcelable(ModuleField.ACCOUNT, account)
                    putString(ModuleField.ACCOUNT_COINS, coinCodes)
                }
            }

            activity.supportFragmentManager.commit {
                add(R.id.fragmentContainerView, fragment)
                addToBackStack(null)
            }
        }
    }
}
