package io.horizontalsystems.bankwallet.modules.restoreconfig

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.modules.balance.token.BirthdayHeightInputField
import io.horizontalsystems.bankwallet.modules.balance.ui.WheelDatePickerBottomSheet
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessor
import io.horizontalsystems.bankwallet.ui.compose.components.captionSB_grey
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreBirthdayHeightScreen(
    blockchainType: BlockchainType,
    onCloseClick: () -> Unit,
    onCloseWithResult: (BirthdayHeightConfig) -> Unit,
    viewModel: RestoreBirthdayHeightViewModel = viewModel(
        factory = RestoreBirthdayHeightViewModel.Factory(blockchainType)
    )
) {
    val uiState = viewModel.uiState
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var textState by rememberSaveable("", stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    uiState.closeWithResult?.let {
        viewModel.onClosed()
        keyboardController?.hide()
        onCloseWithResult.invoke(it)
    }

    LaunchedEffect(uiState.birthdayHeightText) {
        uiState.birthdayHeightText?.let { heightText ->
            textState = TextFieldValue(heightText, TextRange(heightText.length))
        }
    }

    if (showDatePicker) {
        val initialDate = viewModel.getInitialDateForPicker()
        var loading by remember { mutableStateOf(false) }

        WheelDatePickerBottomSheet(
            onDismissRequest = {
                showDatePicker = false
            },
            sheetState = datePickerSheetState,
            loading = loading,
            initialDate = initialDate,
            startDate = uiState.firstBlockDate,
            endDate = LocalDate.now(),
            onConfirm = { day, month, year ->
                coroutineScope.launch {
                    loading = true
                    viewModel.onDateSelected(day, month, year)
                    datePickerSheetState.hide()
                    showDatePicker = false
                }
            }
        )
    }

    HSScaffold(
        title = blockchainType.title,
        onBack = onCloseClick,
        bottomBar = {
            ButtonsGroupWithShade {
                HSButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.Button_Done),
                    variant = ButtonVariant.Primary,
                    enabled = uiState.doneButtonEnabled,
                    onClick = { viewModel.onDoneClick() }
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
                text = stringResource(R.string.BirthdayHeight_RestoreDescription),
            )

            Spacer(Modifier.height(12.dp))

            BirthdayHeightInputField(
                textState = textState,
                hint = viewModel.hintText,
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
                    showDatePicker = true
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
