package io.horizontalsystems.bankwallet.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SetPinPage(val input: Input? = null) : HSPage(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        PinSet(
            title = stringResource(R.string.PinSet_Title),
            description = stringResource(input?.descriptionResId ?: R.string.PinSet_Info),
            dismissWithSuccess = {
                resultEventBus.sendResult(Result(true))
                navigation.removeLastOrNull()
            },
            onBackPress = { navigation.removeLastOrNull() }
        )
    }

    @Serializable
    data class Input(val descriptionResId: Int)

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}
