package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportant
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader

class BackupRequiredDialog : BaseComposableBottomSheetFragment() {

    interface Listener {
        fun onClickBackup(account: Account)
    }

    private val account by lazy {
        requireArguments().getParcelable<Account>(ACCOUNT)
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    BottomSheetHeader(
                        iconPainter = painterResource(R.drawable.ic_attention_red_24),
                        title = stringResource(R.string.ManageAccount_BackupRequired_Title),
                        subtitle = account?.name,
                        onCloseClick = { close() }
                    ) {
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = ComposeAppTheme.colors.steel10
                        )
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            TextImportant(text = stringResource(R.string.ManageAccount_BackupRequired_Description))
                        }
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = ComposeAppTheme.colors.steel10
                        )
                        ButtonPrimaryYellow(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            title = getString(R.string.ManageAccount_RecoveryPhraseBackup),
                            onClick = {
                                account?.let { listener?.onClickBackup(it) }
                                dismiss()
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val ACCOUNT = "account"

        fun show(fragmentManager: FragmentManager, account: Account) {
            val fragment = BackupRequiredDialog().apply {
                arguments = bundleOf(ACCOUNT to account)
            }

            fragmentManager.beginTransaction().apply {
                add(fragment, "backup_required_dialog")
                commitAllowingStateLoss()
            }
        }
    }

}
