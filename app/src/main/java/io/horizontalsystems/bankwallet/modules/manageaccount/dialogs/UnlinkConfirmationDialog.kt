package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryRed
import io.horizontalsystems.bankwallet.ui.compose.components.CellCheckboxLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader

class UnlinkConfirmationDialog : BaseComposableBottomSheetFragment() {

    interface Listener {
        fun onUnlinkConfirm()
    }

    private val checkboxItems by lazy {
        requireArguments().getStringArrayList(CHECKBOX_ITEMS) ?: listOf()
    }

    private val accountName by lazy { requireArguments().getString(ACCOUNT_NAME) }

    private val viewModel by viewModels<UnlinkConfirmationDialogViewModel> {
        UnlinkConfirmationDialogModule.Factory(checkboxItems)
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
                    BottomSheetScreen()
                }
            }
        }
    }

    @Composable
    private fun BottomSheetScreen() {
        val items by viewModel.itemsLiveData.observeAsState(listOf())
        val buttonEnabled by viewModel.buttonEnabledLiveData.observeAsState(false)

        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_attention_red_24),
            title = stringResource(R.string.ManageKeys_Delete_Title),
            subtitle = accountName,
            onCloseClick = { close() }
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )

            items.forEachIndexed { index, item ->
                CellCheckboxLawrence(
                    borderBottom = true,
                    onClick = { viewModel.updateItem(index, item, !item.checked) }
                ) {
                    HsCheckbox(
                        checked = item.checked,
                        onCheckedChange = { checked ->
                            viewModel.updateItem(index, item, checked)
                        },
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = item.text,
                        style = ComposeAppTheme.typography.subhead2,
                        color = ComposeAppTheme.colors.leah
                    )
                }
            }

            ButtonPrimaryRed(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                title = getString(R.string.ManageKeys_Delete_FromPhone),
                onClick = {
                    listener?.onUnlinkConfirm()
                    dismiss()
                },
                enabled = buttonEnabled
            )

        }
    }

    companion object {
        private const val ACCOUNT_NAME = "account_name"
        private const val CHECKBOX_ITEMS = "checkbox_items"

        fun show(
            fragmentManager: FragmentManager,
            accountName: String,
            checkboxItems: List<String>
        ) {
            val fragment = UnlinkConfirmationDialog().apply {
                arguments = bundleOf(
                    ACCOUNT_NAME to accountName,
                    CHECKBOX_ITEMS to ArrayList(checkboxItems)
                )
            }

            fragmentManager.beginTransaction().apply {
                add(fragment, "unlink_confirmation_dialog")
                commitAllowingStateLoss()
            }
        }
    }
}

class UnlinkConfirmationDialogViewModel(checkboxItems: List<String>) : ViewModel() {
    val items = mutableListOf<CheckBoxItem>().apply {
        addAll(checkboxItems.map { CheckBoxItem(it) })
    }
    val itemsLiveData = MutableLiveData(items.toList())
    val buttonEnabledLiveData = MutableLiveData(false)

    fun updateItem(index: Int, item: CheckBoxItem, checked: Boolean) {
        items.removeAt(index)
        items.add(index, CheckBoxItem(item.text, checked))
        itemsLiveData.postValue(items.toList())
        buttonEnabledLiveData.postValue(items.all { it.checked })
    }
}

object UnlinkConfirmationDialogModule {
    class Factory(private val checkboxItems: List<String>) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UnlinkConfirmationDialogViewModel(checkboxItems) as T
        }
    }
}

data class CheckBoxItem(val text: String, val checked: Boolean = false)
