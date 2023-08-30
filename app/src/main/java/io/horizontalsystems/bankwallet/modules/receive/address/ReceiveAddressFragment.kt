package cash.p.terminal.modules.receive.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.entities.ViewState
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.D1
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.HsIconButton
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.TextImportantWarning
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_grey
import cash.p.terminal.ui.compose.components.body_jacob
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.parcelable

class ReceiveAddressFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            val navController = findNavController()
            try {
                val wallet = arguments?.parcelable<Wallet>(WALLET_KEY) ?: throw ReceiveAddressModule.NoWalletData()
                val popupDestinationId = arguments?.getInt(POPUP_DESTINATION_ID_KEY)

                val viewModel by viewModels<ReceiveAddressViewModel> {
                    ReceiveAddressModule.Factory(wallet)
                }
                setContent {
                    ReceiveAddressScreen(
                        viewModel = viewModel,
                        navController = navController,
                        onCloseClick = {
                            popupDestinationId?.let { destinationId ->
                                if (destinationId != 0) {
                                    navController.popBackStack(destinationId, true)
                                } else {
                                    navController.popBackStack()
                                }
                            }
                        }
                    )
                }
            } catch (t: Throwable) {
                Toast.makeText(App.instance, t.message ?: t.javaClass.simpleName, Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        }
    }

    companion object {
        const val WALLET_KEY = "wallet_key"
        const val POPUP_DESTINATION_ID_KEY = "popup_destination_id_key"

        fun params(wallet: Wallet, popupDestination: Int? = null): Bundle {
            return bundleOf(
                WALLET_KEY to wallet,
                POPUP_DESTINATION_ID_KEY to popupDestination,
            )
        }
    }

}

@Composable
private fun ReceiveAddressScreen(
    viewModel: ReceiveAddressViewModel,
    navController: NavController,
    onCloseClick: () -> Unit
) {

    val localView = LocalView.current
    val uiState = viewModel.uiState

    uiState.popupWarningItem?.let { warning ->
        LaunchedEffect(uiState.popupWarningItem) {
            viewModel.popupShown()
            val args = NotActiveWarningDialog.prepareParams(warning.title, warning.description)
            navController.slideFromBottom(R.id.notActiveAccountDialog, args)
        }
    }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Deposit_Title, uiState.coinCode),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Done),
                            onClick = onCloseClick
                        )
                    )
                )
            }
        ) {
            Crossfade(uiState.viewState, label = "") { viewState ->
                Column(Modifier.padding(it)) {
                    when (viewState) {
                        is ViewState.Error -> {
                            ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                        }

                        ViewState.Loading -> {
                            Loading()
                        }

                        ViewState.Success -> {
                            val qrCodeBitmap = TextHelper.getQrCodeBitmap(uiState.address)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                VSpacer(12.dp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(24.dp))
                                        .padding(horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    VSpacer(32.dp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ComposeAppTheme.colors.white)
                                            .size(150.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        qrCodeBitmap?.let {
                                            Image(
                                                modifier = Modifier
                                                    .clickable {
                                                        TextHelper.copyText(uiState.address)
                                                        HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
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
                                                    painter = painterResource(id = R.drawable.launcher_main_preview),
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    }
                                    VSpacer(12.dp)
                                    D1(
                                        text = uiState.qrDescription,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    VSpacer(24.dp)
                                }
                                VSpacer(12.dp)
                                CellUniversalLawrenceSection(uiState.descriptionItems) { item ->
                                    when (item) {
                                        is ReceiveAddressModule.DescriptionItem.Value -> {
                                            RowUniversal(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                            ) {
                                                body_grey(
                                                    text = item.title,
                                                )
                                                body_leah(
                                                    text = item.value,
                                                    modifier = Modifier
                                                        .padding(start = 16.dp)
                                                        .weight(1f),
                                                    textAlign = TextAlign.End
                                                )
                                            }
                                        }

                                        is ReceiveAddressModule.DescriptionItem.ValueInfo -> {
                                            RowUniversal(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                            ) {
                                                body_grey(
                                                    text = item.title,
                                                )
                                                HSpacer(8.dp)
                                                HsIconButton(
                                                    modifier = Modifier
                                                        .padding(end = 8.dp)
                                                        .size(20.dp),
                                                    onClick = {
                                                        val args = NotActiveWarningDialog.prepareParams(item.infoTitle, item.infoText, false)
                                                        navController.slideFromBottom(R.id.notActiveAccountDialog, args)
                                                    }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_info_20),
                                                        contentDescription = "info button",
                                                        tint = ComposeAppTheme.colors.grey,
                                                    )
                                                }
                                                body_jacob(
                                                    text = item.value,
                                                    modifier = Modifier
                                                        .padding(start = 16.dp)
                                                        .weight(1f),
                                                    textAlign = TextAlign.End
                                                )
                                            }
                                        }
                                    }
                                }
                                VSpacer(16.dp)
                                TextImportantWarning(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    text = stringResource(R.string.Balance_ReceiveWarningText, uiState.coinCode)
                                )
                                VSpacer(32.dp)
                            }

                            ButtonsGroupWithShade {
                                Column(Modifier.padding(horizontal = 24.dp)) {
                                    ButtonPrimaryYellow(
                                        modifier = Modifier.fillMaxWidth(),
                                        title = stringResource(R.string.Button_Copy),
                                        onClick = {
                                            TextHelper.copyText(uiState.address)
                                            HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                                        },
                                    )
                                    VSpacer(16.dp)
                                    ButtonPrimaryDefault(
                                        modifier = Modifier.fillMaxWidth(),
                                        title = stringResource(R.string.Button_Close),
                                        onClick = onCloseClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}