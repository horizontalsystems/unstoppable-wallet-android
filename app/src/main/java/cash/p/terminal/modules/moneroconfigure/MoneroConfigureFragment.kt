package cash.p.terminal.modules.moneroconfigure

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.FormsInput
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.getInput
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellMultilineLawrenceSection
import cash.p.terminal.ui_compose.components.HeaderText
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.caption_lucian
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.title3_leah
import cash.p.terminal.ui_compose.findNavController
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.chartview.rememberAsyncImagePainterWithFallback
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.imageUrl
import cash.p.terminal.navigation.setNavigationResultX
import kotlinx.parcelize.Parcelize
import org.koin.compose.viewmodel.koinViewModel

class MoneroConfigureFragment : BaseComposeFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.onBackPressedDispatcher?.addCallback(this) {
            close(findNavController())
        }
    }

    @Composable
    override fun GetContent(navController: NavController) {
        val initialConfig = navController.getInput<Input>()?.initialConfig
        val viewModel: MoneroConfigureViewModel = koinViewModel()
        LaunchedEffect(initialConfig) {
            viewModel.setInitialConfig(initialConfig)
        }
        MoneroConfigureScreen(
            onCloseWithResult = { closeWithConfigt(it, navController) },
            onCloseClick = { close(navController) },
            onRestoreNew = viewModel::onRestoreNew,
            onSetBirthdayHeight = viewModel::setBirthdayHeight,
            onDoneClick = viewModel::onDoneClick,
            uiState = viewModel.uiState,
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
fun MoneroConfigureScreen(
    onCloseClick: () -> Unit,
    onCloseWithResult: (TokenConfig) -> Unit,
    onRestoreNew: (Boolean) -> Unit,
    onSetBirthdayHeight: (String) -> Unit,
    onDoneClick: () -> Unit,
    uiState: MoneroConfigUIState,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets
) {

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.closeWithResult) {
        uiState.closeWithResult?.let {
            keyboardController?.hide()
            onCloseWithResult(it)
        }
    }

    var textState by rememberSaveable("", stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
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
                                    onRestoreNew(true)
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
                                checked = !uiState.restoreAsNew,
                                onClick = {
                                    onRestoreNew(false)
                                    textState =
                                        textState.copy(text = "", selection = TextRange(0))
                                    focusManager.clearFocus()
                                }
                            )
                        },
                    )
                )
                if (!uiState.restoreAsNew) {
                    Spacer(Modifier.height(16.dp))
                    HeaderText(stringResource(id = R.string.restoreheight_title))
                    FormsInput(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        initial = uiState.birthdayHeight,
                        pasteEnabled = false,
                        singleLine = true,
                        hint = stringResource(R.string.restoreheight_hint),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        onValueChange = onSetBirthdayHeight
                    )
                    uiState.errorHeight?.let { errorText ->
                        Spacer(Modifier.height(8.dp))
                        caption_lucian(
                            modifier = Modifier.padding(horizontal = 32.dp),
                            text = errorText
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Done),
                    onClick = onDoneClick
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
                        model = BlockchainType.Monero.imageUrl,
                        error = painterResource(R.drawable.ic_platform_placeholder_32)
                    ),
                    contentDescription = null
                )
                title3_leah(
                    text = stringResource(R.string.restore_monero),
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

@Preview
@Composable
private fun Preview_MoneroConfigure() {
    ComposeAppTheme(darkTheme = false) {
        MoneroConfigureScreen(
            onCloseClick = {},
            onCloseWithResult = {},
            onRestoreNew = {},
            onSetBirthdayHeight = {},
            onDoneClick = {},
            uiState = MoneroConfigUIState(
                birthdayHeight = "",
                restoreAsNew = true,
                closeWithResult = null,
                errorHeight = null
            ),
        )
    }
}
