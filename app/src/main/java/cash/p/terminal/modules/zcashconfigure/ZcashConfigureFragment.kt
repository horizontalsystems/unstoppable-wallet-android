package cash.p.terminal.modules.zcashconfigure

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.addCallback
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.enablecoin.restoresettings.TokenConfig
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.navigation.setNavigationResultX
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.FormsInput
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.getInput
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.TransparentModalBottomSheet
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellMultilineLawrenceSection
import cash.p.terminal.ui_compose.components.HeaderText
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.caption_lucian
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.title3_leah
import cash.p.terminal.ui_compose.findNavController
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.chartview.rememberAsyncImagePainterWithFallback
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.imageUrl
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.koin.compose.viewmodel.koinViewModel

class ZcashConfigureFragment : BaseComposeFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.onBackPressedDispatcher?.addCallback(this) {
            close(findNavController())
        }
    }

    @Composable
    override fun GetContent(navController: NavController) {
        val initialConfig = navController.getInput<Input>()?.initialConfig
        ZcashConfigureScreen(
            initialConfig = initialConfig,
            onCloseWithResult = { closeWithConfigt(it, navController) },
            onCloseClick = { close(navController) }
        )
    }

    private fun closeWithConfigt(config: TokenConfig, navController: NavController) {
        navController.setNavigationResultX(Result(config))
        navController.popBackStack()
    }

    private fun close(navController: NavController) {
        navController.setNavigationResultX(Result(null))
        navController.popBackStack()
    }

    @Parcelize
    data class Result(val config: TokenConfig?) : Parcelable

    @Parcelize
    data class Input(val initialConfig: TokenConfig?) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZcashConfigureScreen(
    onCloseClick: () -> Unit,
    onCloseWithResult: (TokenConfig) -> Unit,
    initialConfig: TokenConfig? = null,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets
) {
    val viewModel: ZcashConfigureViewModel = koinViewModel()

    val uiState = viewModel.uiState
    var showSlowSyncWarning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(initialConfig) {
        viewModel.setInitialConfig(initialConfig)
    }

    uiState.closeWithResult?.let {
        viewModel.onClosed()
        keyboardController?.hide()
        onCloseWithResult.invoke(it)
    }

    var textState by rememberSaveable("", stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    if (showSlowSyncWarning) {
        TransparentModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                showSlowSyncWarning = false
            }
        ) {
            SlowSyncWarningBottomSheet(
                text = stringResource(R.string.Restore_ZCash_SlowSyncWarningText),
                onContinueClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showSlowSyncWarning = false
                        }
                    }
                    viewModel.restoreAsOld()
                },
                onCloseClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showSlowSyncWarning = false
                        }
                    }
                },
            )
        }
    }
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = { ZcashAppBar(onCloseClick = onCloseClick) }
    ) {
        Column(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .windowInsetsPadding(windowInsets)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(12.dp))
                CellMultilineLawrenceSection(
                    listOf(
                        {
                            OptionCell(
                                title = stringResource(R.string.Restore_ZCash_NewWallet),
                                subtitle = stringResource(R.string.Restore_ZCash_NewWallet_Description),
                                checked = uiState.restoreAsNew,
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
                                checked = uiState.restoreAsOld,
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

                if (!uiState.restoreAsNew) {
                    Spacer(Modifier.height(24.dp))
                    HeaderText(text = stringResource(R.string.restore_birthday_height_or_date))

                    FormsInput(
                        initial = uiState.birthdayHeight,
                        pasteEnabled = false,
                        singleLine = true,
                        hint = stringResource(R.string.restoreheight_hint),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        onValueChange = viewModel::setBirthdayHeight,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    uiState.errorHeight?.let { errorText ->
                        Spacer(Modifier.height(8.dp))
                        caption_lucian(
                            modifier = Modifier.padding(horizontal = 32.dp),
                            text = errorText
                        )
                    }

                    InfoText(
                        text = stringResource(R.string.Restore_ZCash_BirthdayHeight_Hint),
                    )
                }

                Spacer(Modifier.height(24.dp))
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(
                        if (uiState.loading) {
                            R.string.Alert_Loading
                        } else {
                            R.string.Button_Done
                        }
                    ),
                    onClick = { viewModel.onDoneClick() },
                    enabled = !uiState.loading && uiState.doneButtonEnabled
                )
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
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
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
    AppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp),
                    painter = rememberAsyncImagePainterWithFallback(
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
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close_24,
                onClick = onCloseClick
            )
        )
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

@Preview
@Composable
private fun Preview_ZcashConfigure() {
    ComposeAppTheme(darkTheme = false) {
        ZcashConfigureScreen(
            onCloseClick = {},
            onCloseWithResult = {},
            initialConfig = null
        )
    }
}
