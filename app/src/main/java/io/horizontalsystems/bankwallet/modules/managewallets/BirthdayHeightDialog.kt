package io.horizontalsystems.bankwallet.modules.managewallets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.helpers.HudHelper

class BirthdayHeightDialog(
    val blockchainIcon: ImageSource,
    val blockchainName: String,
    val birthdayHeight: String
) : BaseComposableBottomSheetFragment() {

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
                ComposeAppTheme {
                    BottomSheetHeader(
                        iconPainter = blockchainIcon.painter(),
                        title = blockchainName,
                        onCloseClick = { close() }
                    ) {
                        Spacer(Modifier.height(12.dp))
                        CellUniversalLawrenceSection(listOf(birthdayHeight), showFrame = true) {
                            RowUniversal(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalPadding = 0.dp
                            ) {
                                val view = LocalView.current
                                val clipboardManager = LocalClipboardManager.current
                                body_leah(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(R.string.Restore_BirthdayHeight),
                                )
                                ButtonSecondaryDefault(
                                    modifier = Modifier.padding(start = 16.dp),
                                    title = birthdayHeight,
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(birthdayHeight))
                                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(44.dp))
                    }
                }
            }
        }
    }

}
