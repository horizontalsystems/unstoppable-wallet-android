package io.horizontalsystems.bankwallet.modules.multiswap.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellGroup
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.core.findNavController

class RequestRefundDialog : BaseComposableBottomSheetFragment() {
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
                val navController = findNavController()

                ComposeAppTheme {
                    RequestRefundScreen(navController)
                }
            }
        }
    }
}

// TODO: HARDCODED STUB DATA — wire to ViewModel once refund flow is connected
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestRefundScreen(navController: NavController) {
    BottomSheetContent(
        onDismissRequest = {
            navController.popBackStack()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        buttons = {
            HSButton(
                modifier = Modifier.fillMaxWidth(),
                title = "Copy details",
                variant = ButtonVariant.Primary,
                onClick = {
                    // TODO: copy swap details to clipboard
                    navController.popBackStack()
                },
            )
        },
        content = {
            BottomSheetHeaderV3(
                title = stringResource(R.string.SwapInfo_RequestRefund),
            )
            TextBlock(stringResource(R.string.SwapInfo_RequestRefundDescription))
            VSpacer(8.dp)
            CellGroup(paddingValues = PaddingValues(horizontal = 16.dp)) {
                CellSecondary(
                    middle = {
                        CellMiddleInfo(subtitle = stringResource(R.string.SwapInfo_SwapId).hs)
                    },
                    right = {
                        CellRightInfo(titleSubheadSb = "0x1234...abcd".hs)
                    },
                )
                CellSecondary(
                    middle = {
                        CellMiddleInfo(subtitle = stringResource(R.string.SwapInfo_Amount).hs)
                    },
                    right = {
                        CellRightInfo(titleSubheadSb = "0.11 BTC".hs)
                    },
                )
                CellSecondary(
                    middle = {
                        CellMiddleInfo(subtitle = stringResource(R.string.SwapInfo_RefundAddress).hs)
                    },
                    right = {
                        CellRightInfo(titleSubheadSb = "0x1v234...abcd".hs)
                    },
                )
            }
            VSpacer(8.dp)
            CellGroup(paddingValues = PaddingValues(horizontal = 16.dp)) {
                CellPrimary(
                    left = {
                        CellLeftImage(
                            painter = painterResource(R.drawable.ic_telegram_24),
                            type = ImageType.Rectangle,
                            size = 24
                        )
                    },
                    middle = {
                        CellMiddleInfo(
                            subtitle = "Telegram".hs(color = ComposeAppTheme.colors.leah),
                            description = "test".hs
                        )
                    },
                    right = {
                        CellRightNavigation()
                    },
                    onClick = {
                        // TODO: open provider Telegram channel
                    },
                )
                HsDivider()
                CellPrimary(
                    left = {
                        CellLeftImage(
                            painter = painterResource(R.drawable.mail_24),
                            type = ImageType.Rectangle,
                            size = 24
                        )
                    },
                    middle = {
                        CellMiddleInfo(
                            subtitle = "Email".hs(color = ComposeAppTheme.colors.leah),
                            description = "test".hs
                        )
                    },
                    right = {
                        CellRightNavigation()
                    },
                    onClick = {
                        // TODO: open mailto: link
                    },
                )
            }
        },
    )
}