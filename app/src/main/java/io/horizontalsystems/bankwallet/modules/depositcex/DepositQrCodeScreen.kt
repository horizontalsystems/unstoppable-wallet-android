package io.horizontalsystems.bankwallet.modules.depositcex

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.core.providers.CexDepositNetwork
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.receive.address.adaptiveIconPainterResource
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DepositQrCodeScreen(
    onNavigateBack: (() -> Unit)?,
    onClose: () -> Unit,
    cexAsset: CexAsset,
    network: CexDepositNetwork?
) {
    val viewModel =
        viewModel<DepositAddressViewModel>(factory = DepositAddressViewModel.Factory(cexAsset, network?.id))

    val uiState = viewModel.uiState
    val address = uiState.address
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    if (address?.tag != null && address.tag.isNotEmpty()) {
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
                    text = stringResource(R.string.CexDeposit_MemoAlertText),
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
                        title = stringResource(R.string.CexDeposit_Title, cexAsset.id),
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
                Crossfade(targetState = uiState.loading, label = "") { loading ->
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
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(ComposeAppTheme.colors.white)
                                                    .size(40.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    modifier = Modifier.size(32.dp),
                                                    painter = adaptiveIconPainterResource(
                                                        id = R.mipmap.launcher_main,
                                                        fallbackDrawable = R.drawable.launcher_main_preview
                                                    ),
                                                    contentDescription = null
                                                )
                                            }
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
                                network?.let { network ->
                                    composableItems.add {
                                        DetailCell(
                                            title = stringResource(R.string.CexDeposit_Network),
                                            value = network.networkName,
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

                                CellUniversalLawrenceSection(composableItems = composableItems)

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
                                        title = stringResource(R.string.Button_Copy),
                                        onClick = {
                                            TextHelper.copyText(address.address)
                                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                                        },
                                    )
                                    VSpacer(16.dp)
                                    ButtonPrimaryDefault(
                                        modifier = Modifier.fillMaxWidth(),
                                        title = stringResource(R.string.Button_Share),
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
    RowUniversal(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        subhead2_grey(title)
        HSpacer(16.dp)
        subhead1_leah(
            modifier = Modifier.weight(1f),
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