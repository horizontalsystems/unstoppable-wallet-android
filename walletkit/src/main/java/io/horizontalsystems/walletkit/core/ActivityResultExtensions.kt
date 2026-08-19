package io.horizontalsystems.walletkit.core

import android.content.ActivityNotFoundException
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.helpers.HudHelper

// The system file picker (DocumentsUI) can be disabled or missing on some devices;
// launch() then throws ActivityNotFoundException straight out of the click handler.
// Returns false when the launch failed — the result callback will never fire, so
// callers driven by state (rather than a click) must reset that state themselves.
fun <I> ActivityResultLauncher<I>.launchSafe(input: I, view: View): Boolean {
    return try {
        launch(input)
        true
    } catch (e: ActivityNotFoundException) {
        HudHelper.showErrorMessage(view, R.string.Error_FilePickerNotFound)
        false
    }
}
