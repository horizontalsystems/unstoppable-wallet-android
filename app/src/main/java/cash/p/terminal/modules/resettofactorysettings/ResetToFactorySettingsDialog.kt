package cash.p.terminal.modules.resettofactorysettings

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.HsCheckbox
import cash.p.terminal.ui_compose.BaseComposableBottomSheetFragment
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.components.ButtonPrimaryRed
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.findNavController
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.setNavigationResultX
import kotlinx.parcelize.Parcelize

class ResetToFactorySettingsDialog : BaseComposableBottomSheetFragment() {
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
                val navController = findNavController()

                ComposeAppTheme {
                    ResetToFactorySettingsScreen(navController)
                }
            }
        }
    }

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}


@Composable
private fun ResetToFactorySettingsScreen(navController: NavController) {
    val confirmations = remember {
        listOf(
            TranslatableString.ResString(R.string.reset_card_to_factory_condition_1),
            TranslatableString.ResString(R.string.reset_card_to_factory_condition_2)
        )
    }
    var checkedItems by remember { mutableStateOf(setOf<Int>()) }

    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_attention_red_24),
        title = stringResource(R.string.reset_to_factory_settings),
        onCloseClick = {
            navController.popBackStack()
        }
    ) {
        Spacer(Modifier.height(12.dp))
        CellUniversalLawrenceSection(confirmations, showFrame = true) { item ->
            val itemId = item.id
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    checkedItems = if (itemId in checkedItems) {
                        checkedItems - itemId
                    } else {
                        checkedItems + itemId
                    }
                }
            ) {
                HsCheckbox(
                    checked = itemId in checkedItems,
                    onCheckedChange = { checked ->
                        checkedItems = if (checked) {
                            checkedItems + itemId
                        } else {
                            checkedItems - itemId
                        }
                    },
                )
                Spacer(Modifier.width(16.dp))
                subhead2_leah(
                    text = item.getString()
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        TextImportantWarning(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(id = R.string.reset_card_with_backup_to_factory_message)
        )

        Spacer(Modifier.height(32.dp))
        ButtonPrimaryRed(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.reset),
            onClick = {
                navController.setNavigationResultX(ResetToFactorySettingsDialog.Result(true))
                navController.popBackStack()
            },
            enabled = checkedItems.size == 2
        )
        Spacer(Modifier.height(32.dp))
    }
}
