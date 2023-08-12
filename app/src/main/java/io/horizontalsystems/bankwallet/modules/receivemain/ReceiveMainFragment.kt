package io.horizontalsystems.bankwallet.modules.receivemain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.D1
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class ReceiveMainFragment : BaseFragment() {

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
                ReceiveMainScreen(
                    findNavController()
                )
            }
        }
    }

}

@Composable
private fun ReceiveMainScreen(
    navController: NavController,
) {
    val address = "0x1c6EAa67452C34C95206f5F7C7a6f76Ad81f51"
    val network = "Ethereum"
    val coinCode = "ETH"

    val localView = LocalView.current
    val qrBitmap = TextHelper.getQrCodeBitmap(address)
    val addressHint = stringResource(R.string.Balance_ReceiveAddressHint, coinCode)

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Deposit_Title, coinCode),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Done),
                            onClick = {
                                navController.popBackStack(R.id.receiveTokenSelectFragment, true)
                            }
                        )
                    )
                )
            }
        ) {
            Column(Modifier.padding(it)) {
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
                                .size(150.dp)
                        ) {
                            qrBitmap?.let {
                                Image(
                                    modifier = Modifier
                                        .clickable {
                                            TextHelper.copyText(address)
                                            HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
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
                        D1(
                            text = addressHint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        VSpacer(24.dp)
                    }
                    VSpacer(12.dp)
                    CellUniversalLawrenceSection(
                        listOf(
                            {
                                RowUniversal(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                ) {
                                    body_grey(
                                        text = stringResource(R.string.Balance_Address),
                                    )
                                    subhead2_leah(
                                        text = address,
                                        modifier = Modifier
                                            .padding(start = 16.dp)
                                            .weight(1f)
                                            .clickable {
                                                TextHelper.copyText(address)
                                                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                                            },
                                        textAlign = TextAlign.End
                                    )
                                }
                            }, {
                                RowUniversal(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                ) {
                                    body_grey(
                                        text = stringResource(R.string.Balance_Network),
                                    )
                                    subhead2_leah(
                                        text = network,
                                        modifier = Modifier
                                            .padding(start = 16.dp)
                                            .weight(1f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        )
                    )
                    VSpacer(16.dp)
                    TextImportantWarning(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.Balance_ReceiveWarningText, network)
                    )
                    VSpacer(32.dp)
                }

                ButtonsGroupWithShade {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        ButtonPrimaryYellow(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.Button_Copy),
                            onClick = {
                                TextHelper.copyText(address)
                                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                            },
                        )
                        VSpacer(16.dp)
                        ButtonPrimaryDefault(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.Button_Close),
                            onClick = {
                                navController.popBackStack(R.id.receiveTokenSelectFragment, true)
                            }
                        )
                    }
                }
            }
        }
    }
}
