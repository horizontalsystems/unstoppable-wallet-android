package io.horizontalsystems.walletkit.core

import android.content.ActivityNotFoundException
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.helpers.HudHelper

// The system file picker (DocumentsUI) can be disabled or missing on some devices;
// launch() then throws ActivityNotFoundException straight out of the click handler.
fun <I> ActivityResultLauncher<I>.launchSafe(input: I, view: View) {
    try {
        launch(input)
    } catch (e: ActivityNotFoundException) {
        HudHelper.showErrorMessage(view, R.string.Error_FilePickerNotFound)
    }
}
