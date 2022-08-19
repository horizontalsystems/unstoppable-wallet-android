package io.horizontalsystems.bankwallet.modules.recoveryphrase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class RecoveryPhraseFragment : BaseFragment() {

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
                RecoveryPhraseScreen(
                    account = arguments?.getParcelable(RecoveryPhraseModule.ACCOUNT)!!,
                    onBackPress = { findNavController().popBackStack() },
                    showKeyWarning = { key -> showPrivateKeyCopyWarning(key) },
                )
            }
        }
    }

    private fun showPrivateKeyCopyWarning(key: String) {
        ConfirmationDialog.show(
            title = getString(R.string.RecoveryPhrase_CopyWarning_Title),
            warningText = getString(R.string.ShowKey_PrivateKeyCopyWarning_Text),
            actionButtonTitle = getString(R.string.Button_Ok),
            transparentButtonTitle = getString(R.string.ShowKey_PrivateKeyCopyWarning_Proceed),
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onTransparentButtonClick() {
                    TextHelper.copyText(key)
                    HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
                }

            }
        )
    }
}

@Composable
private fun RecoveryPhraseScreen(
    account: Account,
    onBackPress: () -> Unit,
    showKeyWarning: (String) -> Unit,
    viewModel: RecoveryPhraseViewModel = viewModel(
        factory = RecoveryPhraseModule.Factory(account)
    )
) {

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.RecoveryPhrase_Title),
                navigationIcon = {
                    HsIconButton(onClick = onBackPress) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(Modifier.height(12.dp))
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.RecoveryPhrase_Warning)
                )
                Spacer(Modifier.height(24.dp))
                var hidden by remember { mutableStateOf(true) }
                SeedPhraseList(viewModel.wordsNumbered, hidden) {
                    hidden = it
                }
                Spacer(Modifier.height(24.dp))
                PassphraseCell(viewModel.passphrase, hidden)
            }
            ActionButton(R.string.Alert_Copy) {
                showKeyWarning.invoke(viewModel.words.joinToString(" "))
            }
        }
    }
}

@Composable
fun PassphraseCell(passphrase: String, hidden: Boolean) {
    if (passphrase.isNotBlank()) {
        CellSingleLineLawrenceSection(
            listOf {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_key_phrase_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                    D1(
                        text = stringResource(R.string.ShowKey_Passphrase),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    C2(text = if (hidden) "*****" else passphrase)
                }
            })
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ActionButton(title: Int, onClick: () -> Unit) {
    ButtonsGroupWithShade {
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            title = stringResource(title),
            onClick = onClick,
        )
    }
}

@Composable
fun SeedPhraseList(
    wordsNumbered: List<RecoveryPhraseModule.WordNumbered>,
    hidden: Boolean,
    onClickToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(24.dp))
            .clickable(onClick = { onClickToggle.invoke(!hidden) })
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            mainAxisAlignment = FlowMainAxisAlignment.Center,
            crossAxisSpacing = 16.dp
        ) {
            wordsNumbered.chunked(3).forEach {
                it.forEach { word ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        D7(text = word.number.toString())
                        Spacer(modifier = Modifier.width(8.dp))
                        B2(text = word.word)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }

        if (hidden) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(ComposeAppTheme.colors.tyler),
                contentAlignment = Alignment.Center
            ) {
                subhead2_grey(text = stringResource(R.string.RecoveryPhrase_ShowPhrase))
            }
        }
    }
}
