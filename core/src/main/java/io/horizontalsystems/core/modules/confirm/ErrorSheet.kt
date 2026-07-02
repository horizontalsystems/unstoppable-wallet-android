package io.horizontalsystems.core.modules.confirm

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.horizontalsystems.core.R
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.ui.extensions.HSBottomSheet
import io.horizontalsystems.core.ui.helpers.TextHelper
import io.horizontalsystems.core.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.core.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.core.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.core.uiv3.components.controls.HSButton
import io.horizontalsystems.core.uiv3.components.info.TextBlock
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class ErrorSheet(val input: Input) : HSBottomSheet() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        ErrorBottomSheetScreen(navigation, input.error)
    }

    @Serializable
    @Parcelize
    data class Input(val error: String) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorBottomSheetScreen(
    navigation: HSNavigation,
    error: String
) {
    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = navigation::removeLastOrNull,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            buttons = {
                HSButton(
                    title = stringResource(R.string.Button_CopyError),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    onClick = {
                        TextHelper.copyText(error)
                        navigation.removeLastOrNull()
                    }
                )
            },
            content = {
                BottomSheetHeaderV3(
                    image72 = painterResource(R.drawable.warning_filled_24),
                    imageTint = ComposeAppTheme.colors.lucian,
                    title = stringResource(R.string.Send_UnexpectedError)
                )
                TextBlock(text = stringResource(R.string.Send_UnexpectedError_Description), textAlign = TextAlign.Center)
            }
        )
    }
}

