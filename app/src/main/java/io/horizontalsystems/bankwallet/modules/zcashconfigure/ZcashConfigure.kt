package io.horizontalsystems.bankwallet.modules.zcashconfigure

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.ZCashConfig
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch

class ZcashConfigure : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity?.onBackPressedDispatcher?.addCallback(this) {
            close()
        }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ZcashConfigureScreen(
                    onCloseWithResult = { closeWithConfigt(it) },
                    onCloseClick = { close() }
                )
            }
        }
    }

    private fun closeWithConfigt(config: ZCashConfig) {
        findNavController().setNavigationResult(
            resultBundleKey,
            bundleOf(
                requestResultKey to RESULT_OK,
                zcashConfigKey to config,
            )
        )
        findNavController().popBackStack()
    }

    private fun close() {
        findNavController().setNavigationResult(
            resultBundleKey,
            bundleOf(requestResultKey to RESULT_CANCELLED)
        )
        findNavController().popBackStack()
    }

    companion object {
        const val RESULT_OK = 1
        const val RESULT_CANCELLED = 2
        const val resultBundleKey = "resultBundleKey"
        const val requestResultKey = "requestResultKey"
        const val zcashConfigKey = "zcashConfigKey"
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ZcashConfigureScreen(
    onCloseClick: () -> Unit,
    onCloseWithResult: (ZCashConfig) -> Unit,
    viewModel: ZcashConfigureViewModel = viewModel()
) {
    var showSlowSyncWarning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        confirmStateChange = {
            if (it == ModalBottomSheetValue.Hidden) {
                showSlowSyncWarning = false
            }
            true
        }
    )

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    viewModel.uiState.closeWithResult?.let {
        viewModel.onClosed()
        onCloseWithResult.invoke(it)
    }

    if (showSlowSyncWarning) {
        LaunchedEffect(Unit) {
            sheetState.show()
        }
    }

    var textState by rememberSaveable("", stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    ComposeAppTheme {
        ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetBackgroundColor = ComposeAppTheme.colors.transparent,
            sheetContent = {
                SlowSyncWarningBottomSheet(
                    text = stringResource(R.string.Restore_ZCash_SlowSyncWarningText),
                    onContinueClick = {
                        showSlowSyncWarning = false
                        scope.launch { sheetState.hide() }
                        viewModel.restoreAsOld()
                    },
                    onCloseClick = {
                        showSlowSyncWarning = false
                        scope.launch { sheetState.hide() }
                    },
                )
            }
        ) {
            Scaffold(
                backgroundColor = ComposeAppTheme.colors.tyler,
                topBar = { ZcashAppBar(onCloseClick = onCloseClick) }
            ) {
                Column(modifier = Modifier.padding(it)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Spacer(Modifier.height(12.dp))
                        CellMultilineLawrenceSection(
                            listOf(
                                {
                                    OptionCell(
                                        title = stringResource(R.string.Restore_ZCash_NewWallet),
                                        subtitle = stringResource(R.string.Restore_ZCash_NewWallet_Description),
                                        checked = viewModel.uiState.restoreAsNew,
                                        onClick = {
                                            viewModel.restoreAsNew()
                                            textState =
                                                textState.copy(text = "", selection = TextRange(0))
                                            focusManager.clearFocus()
                                        }
                                    )
                                },
                                {
                                    OptionCell(
                                        title = stringResource(R.string.Restore_ZCash_OldWallet),
                                        subtitle = stringResource(R.string.Restore_ZCash_OldWallet_Description),
                                        checked = viewModel.uiState.restoreAsOld,
                                        onClick = {
                                            showSlowSyncWarning = true
                                            textState =
                                                textState.copy(text = "", selection = TextRange(0))
                                            focusManager.clearFocus()
                                        }
                                    )
                                },
                            )
                        )

                        Spacer(Modifier.height(24.dp))
                        HeaderText(text = stringResource(R.string.Restore_BirthdayHeight))

                        BirthdayHeightInput(
                            textState = textState,
                            focusRequester = focusRequester,
                            textPreprocessor = object : TextPreprocessor {
                                override fun process(text: String): String {
                                    return text.replace("[^0-9]".toRegex(), "")
                                }
                            },
                            onValueChange = { textFieldValue ->
                                textState = textFieldValue
                                viewModel.setBirthdayHeight(textFieldValue.text)
                            }
                        )

                        InfoText(
                            text = stringResource(R.string.Restore_ZCash_BirthdayHeight_Hint),
                        )

                        Spacer(Modifier.height(24.dp))
                    }

                    ButtonsGroupWithShade {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                            title = stringResource(R.string.Button_Done),
                            onClick = { viewModel.onDoneClick() },
                            enabled = viewModel.uiState.doneButtonEnabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionCell(
    title: String,
    subtitle: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            body_leah(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            subhead2_grey(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(R.drawable.ic_checkmark_20),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
fun ZcashAppBar(
    onCloseClick: () -> Unit,
) {
    TopAppBar(
        modifier = Modifier.height(56.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    modifier = Modifier.padding(end = 16.dp).size(24.dp),
                    painter = rememberAsyncImagePainter(
                        model = BlockchainType.Zcash.imageUrl,
                        error = painterResource(R.drawable.ic_platform_placeholder_32)
                    ),
                    contentDescription = null
                )
                title3_leah(
                    text = stringResource(R.string.Restore_ZCash),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
        actions = {
            AppBarMenuButton(
                icon = R.drawable.ic_close,
                onClick = onCloseClick,
                description = stringResource(R.string.Button_Close)
            )
        },
        elevation = 0.dp
    )
}

@Composable
private fun SlowSyncWarningBottomSheet(
    text: String,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_attention_24),
        title = stringResource(R.string.Alert_TitleWarning),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
        onCloseClick = onCloseClick
    ) {
        TextImportantWarning(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = text
        )

        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp),
            title = stringResource(id = R.string.Button_Continue),
            onClick = onContinueClick
        )

        ButtonPrimaryTransparent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            title = stringResource(id = R.string.Button_Cancel),
            onClick = onCloseClick
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun BirthdayHeightInput(
    textState: TextFieldValue,
    textPreprocessor: TextPreprocessor = TextPreprocessorImpl,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .height(44.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        BasicTextField(
            modifier = Modifier
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
                        modifier = Modifier.focusRequester(focusRequester),
                        text = "000000000",
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
                innerTextField()
            },
        )
    }
}

@Preview
@Composable
private fun Preview_ZcashConfigure() {
    ComposeAppTheme(darkTheme = false) {
        ZcashConfigureScreen({}, {})
    }
}
