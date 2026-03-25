package io.horizontalsystems.bankwallet.modules.evmfee

import android.os.Parcelable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.InfoTextBody
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import kotlinx.parcelize.Parcelize

class FeeSettingsInfoDialog(val input: Input) : BaseComposableBottomSheetFragment() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        FeeSettingsInfoScreen(input.title, input.text) { navController.removeLastOrNull() }
    }

    @Parcelize
    data class Input(val title: String, val text: String) : Parcelable
}

@Composable
fun FeeSettingsInfoScreen(title: String?, text: String?, onCloseClick: () -> Unit) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_info_24),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.grey),
        title = title ?: "",
        onCloseClick = onCloseClick
    ) {
        InfoTextBody(text = text ?: "")
        Spacer(modifier = Modifier.height(52.dp))
    }
}
