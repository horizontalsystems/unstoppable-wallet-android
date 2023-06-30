package cash.p.terminal.modules.depositcex

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui.compose.components.CellSingleLineLawrenceSection
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.TextImportantError
import cash.p.terminal.ui.compose.components.TextImportantWarning
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_grey
import cash.p.terminal.ui.compose.components.subhead1_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.extensions.BottomSheetHeader
import cash.p.terminal.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DepositQrCodeScreen(
    onNavigateBack: (() -> Unit)?,
    onClose: () -> Unit,
    cexAsset: CexAsset,
    networkId: String?
) {
    val viewModel =
        viewModel<DepositAddressViewModel>(factory = DepositAddressViewModel.Factory(cexAsset, networkId))

    val uiState = viewModel.uiState
    val address = uiState.address
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    if(address?.tag != null && address.tag.isNotEmpty()) {
        LaunchedEffect(Unit) {
            modalBottomSheetState.show()
        }
    }

    ComposeAppTheme {
        ModalBottomSheetLayout(
            sheetState = modalBottomSheetState,
            sheetBackgroundColor = ComposeAppTheme.colors.transparent,
            sheetContent = {
                WarningBottomSheet(
                    text = stringResource(R.string.CexDeposit_MemoAlertText,),
                    onButtonClick = {
                        coroutineScope.launch { modalBottomSheetState.hide() }
                    }
                )
            },
        ) {
            Scaffold(
                backgroundColor = ComposeAppTheme.colors.tyler,
                topBar = {
                    val navigationIcon: @Composable (() -> Unit)? = onNavigateBack?.let {
                        {
                            HsBackButton(onClick = onNavigateBack)
                        }
                    }
                    AppBar(
                        title = TranslatableString.ResString(R.string.CexDeposit_Title, cexAsset.id),
                        navigationIcon = navigationIcon,
                        menuItems = listOf(
                            MenuItem(
                                title = TranslatableString.ResString(R.string.Button_Done),
                                onClick = onClose
                            )
                        )
                    )
                }
            ) {
                Crossfade(targetState = uiState.loading) { loading ->
                    Column(modifier = Modifier.padding(it)) {
                        if (loading) {
                            Loading()
                        } else if (address != null) {
                            val qrBitmap = TextHelper.getQrCodeBitmap(address.address)
                            val view = LocalView.current
                            val context = LocalContext.current

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                VSpacer(12.dp)
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(24.dp))
                                        .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ComposeAppTheme.colors.white)
                                            .size(150.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        qrBitmap?.let {
                                            Image(
                                                modifier = Modifier
                                                    .clickable {
                                                        TextHelper.copyText(address.address)
                                                        HudHelper.showSuccessMessage(
                                                            view,
                                                            R.string.Hud_Text_Copied
                                                        )
                                                    }
                                                    .padding(8.dp)
                                                    .fillMaxSize(),
                                                bitmap = it.asImageBitmap(),
                                                contentScale = ContentScale.FillWidth,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                    VSpacer(12.dp)
                                    subhead2_grey(
                                        text = stringResource(R.string.CexDeposit_AddressDescription, cexAsset.id),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                VSpacer(12.dp)
                                val composableItems: MutableList<@Composable () -> Unit> = mutableListOf()
                                composableItems.add {
                                    DetailCell(
                                        title = stringResource(R.string.Deposit_Address),
                                        value = address.address,
                                    )
                                }
                                networkId?.let { networkId ->
                                    composableItems.add {
                                        DetailCell(
                                            title = stringResource(R.string.CexDeposit_Network),
                                            value = networkId,
                                        )
                                    }
                                }
                                if (address.tag.isNotEmpty()) {
                                    composableItems.add {
                                        DetailCell(
                                            title = stringResource(R.string.CexDeposit_Memo),
                                            value = address.tag,
                                            onCopy = {
                                                TextHelper.copyText(address.tag)
                                                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                                            }
                                        )
                                    }
                                }

                                CellSingleLineLawrenceSection(composableItems = composableItems)

                                if (address.tag.isNotEmpty()) {
                                    TextImportantError(
                                        modifier = Modifier.padding(16.dp),
                                        text = stringResource(R.string.CexDeposit_MemoWarning, cexAsset.id)
                                    )
                                } else {
                                    TextImportantWarning(
                                        modifier = Modifier.padding(16.dp),
                                        text = stringResource(R.string.CexDeposit_DepositWarning, cexAsset.id)
                                    )
                                }

                                VSpacer(24.dp)
                            }
                            ButtonsGroupWithShade {
                                Column(Modifier.padding(horizontal = 24.dp)) {
                                    ButtonPrimaryYellow(
                                        modifier = Modifier.fillMaxWidth(),
                                        title = stringResource(R.string.CexDeposit_CopyAddress),
                                        onClick = {
                                            TextHelper.copyText(address.address)
                                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                                        },
                                    )
                                    VSpacer(16.dp)
                                    ButtonPrimaryDefault(
                                        modifier = Modifier.fillMaxWidth(),
                                        title = stringResource(R.string.CexDeposit_ShareAddress),
                                        onClick = {
                                            ShareCompat.IntentBuilder(context)
                                                .setType("text/plain")
                                                .setText(address.address)
                                                .startChooser()
                                        }
                                    )
                                }
                            }
                        } else if (uiState.error != null) {
                            ListEmptyView(
                                text = uiState.error.localizedMessage ?: stringResource(R.string.Error),
                                icon = R.drawable.ic_sync_error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCell(
    title: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        body_grey(text = title)

        subhead1_leah(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = value,
            textAlign = TextAlign.End,
        )
        onCopy?.let { copy ->
            HSpacer(16.dp)
            ButtonSecondaryCircle(
                icon = R.drawable.ic_copy_20,
                onClick = copy
            )
        }
    }
}

@Composable
private fun WarningBottomSheet(
    text: String,
    onButtonClick: () -> Unit,
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_attention_24),
        title = stringResource(R.string.CexDeposit_Important),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.lucian),
        onCloseClick = onButtonClick
    ) {
        VSpacer(12.dp)
        TextImportantError(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = text
        )
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            title = stringResource(id = R.string.Button_Understand),
            onClick = onButtonClick
        )
    }
}