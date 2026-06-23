package io.horizontalsystems.bankwallet.modules.evmfee

import android.os.Parcelable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.InfoTextBody
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.ui.extensions.HSBottomSheet
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class FeeSettingsInfoSheet(val input: Input) : HSBottomSheet() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        FeeSettingsInfoScreen(input.title, input.text) { navigation.removeLastOrNull() }
    }

    @Serializable
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
