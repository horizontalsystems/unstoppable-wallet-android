package io.horizontalsystems.bankwallet.modules.balance.token

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessor
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey50
import io.horizontalsystems.bankwallet.ui.compose.components.captionSB_grey
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

class EnterBirthdayHeightFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            EnterBirthdayHeightScreen(
                blockchainType = input.blockchainType,
                account = input.account,
                currentBirthdayHeight = input.currentBirthdayHeight,
                onCloseClick = { navController.popBackStack() },
                onRescan = {
                    // TODO: Implement rescan logic
                    navController.popBackStack()
                }
            )
        }
    }

    @Parcelize
    data class Input(
        val blockchainType: BlockchainType,
        val account: Account,
        val currentBirthdayHeight: Long?
    ) : Parcelable
}

@Composable
fun EnterBirthdayHeightScreen(
    blockchainType: BlockchainType,
    account: Account,
    currentBirthdayHeight: Long?,
    onCloseClick: () -> Unit,
    onRescan: (Long?) -> Unit,
    viewModel: EnterBirthdayHeightViewModel = viewModel(
        factory = EnterBirthdayHeightModule.Factory(blockchainType, account, currentBirthdayHeight)
    )
) {
    val uiState = viewModel.uiState
    val focusRequester = remember { FocusRequester() }

    var textState by rememberSaveable("", stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

        HSScaffold(
        title = stringResource(R.string.Restore_BirthdayHeight),
        onBack = onCloseClick,
        bottomBar = {
            ButtonsGroupWithShade {
                HSButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.BirthdayHeight_Rescan),
                    variant = ButtonVariant.Primary,
                    enabled = uiState.rescanButtonEnabled,
                    onClick = { onRescan(uiState.birthdayHeight) }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            InfoText(
                text = stringResource(R.string.BirthdayHeight_Description),
            )

            Spacer(Modifier.height(12.dp))

            BirthdayHeightInputField(
                textState = textState,
                hint = currentBirthdayHeight?.toString() ?: "",
                focusRequester = focusRequester,
                textPreprocessor = object : TextPreprocessor {
                    override fun process(text: String): String {
                        return text.replace("[^0-9]".toRegex(), "")
                    }
                },
                onValueChange = { textFieldValue ->
                    textState = textFieldValue
                    viewModel.setBirthdayHeight(textFieldValue.text)
                },
                onPasteClick = {
                    TextHelper.getCopiedText()?.let { pastedText ->
                        val processed = pastedText.replace("[^0-9]".toRegex(), "")
                        textState = TextFieldValue(processed, TextRange(processed.length))
                        viewModel.setBirthdayHeight(processed)
                    }
                },
                onCalendarClick = {
                    // TODO: Open date picker
                },
                onDeleteClick = {
                    textState = TextFieldValue("")
                    viewModel.setBirthdayHeight("")
                }
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                caption_grey(text = stringResource(R.string.BirthdayHeight_BlockDate))
                Spacer(Modifier.weight(1f))
                captionSB_grey(text = uiState.blockDateText ?: "")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BirthdayHeightInputField(
    textState: TextFieldValue,
    hint: String,
    focusRequester: FocusRequester,
    textPreprocessor: TextPreprocessor,
    onValueChange: (TextFieldValue) -> Unit,
    onPasteClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .height(44.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .padding(vertical = 12.dp)
                .weight(1f),
            value = textState,
            onValueChange = { textFieldValue ->
                val textFieldValueProcessed =
                    textFieldValue.copy(text = textPreprocessor.process(textFieldValue.text))
                onValueChange.invoke(textFieldValueProcessed)
            },
            textStyle = ColoredTextStyle(
                color = ComposeAppTheme.colors.leah,
                textStyle = ComposeAppTheme.typography.body
            ),
            maxLines = 1,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { innerTextField ->
                if (textState.text.isEmpty()) {
                    body_grey50(
                        text = hint,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
                innerTextField()
            },
        )

        if (textState.text.isNotEmpty()) {
            ButtonSecondaryCircle(
                modifier = Modifier.padding(end = 8.dp),
                icon = R.drawable.ic_delete_20,
                onClick = {
                    onDeleteClick()
                    focusRequester.requestFocus()
                }
            )
        } else {
            ButtonSecondaryCircle(
                modifier = Modifier.padding(end = 8.dp),
                icon = R.drawable.ic_date_20,
                onClick = onCalendarClick
            )

            ButtonSecondaryDefault(
                modifier = Modifier.height(28.dp),
                title = stringResource(R.string.Send_Button_Paste),
                onClick = onPasteClick
            )
        }
    }
}
