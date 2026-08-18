package io.horizontalsystems.walletkit.modules.send

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation

/**
 * Holds the confirmation data for a send screen, refreshing it when the screen resumes.
 *
 * The view models build it from amount, address and fee state that is filled in asynchronously and
 * is not persisted anywhere. The back stack is persisted, though, so after process death the user
 * is returned to a confirmation screen whose view model has been recreated empty. Rather than
 * asserting its way to a crash on composition, [provider] reports null and the screen is popped so
 * the user lands back on the send form.
 */
@Composable
fun rememberConfirmationData(
    navigation: HSNavigation,
    provider: () -> SendConfirmationData?
): SendConfirmationData? {
    var confirmationData by remember { mutableStateOf(provider()) }
    var refresh by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        if (refresh) {
            confirmationData = provider()
        }

        onPauseOrDispose {
            refresh = true
        }
    }

    LaunchedEffect(confirmationData) {
        if (confirmationData == null) {
            navigation.removeLastOrNull()
        }
    }

    return confirmationData
}
