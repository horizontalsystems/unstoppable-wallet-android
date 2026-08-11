package io.horizontalsystems.walletkit.modules.walletconnect

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.dapp.core.HSDAppValidation
import io.horizontalsystems.dapp.core.HSDAppVerification
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.AlertCard
import io.horizontalsystems.walletkit.uiv3.components.AlertFormat
import io.horizontalsystems.walletkit.uiv3.components.AlertType

/**
 * Shows the protocol's origin attestation for a dApp.
 *
 * The name and url shown elsewhere on these screens are reported by the dApp about itself and can
 * say anything, so this is the only part a phishing site cannot control. It is deliberately not
 * gated behind the Scam Protection subscription: an impersonation signal is precisely what a user
 * has no way to check by reading the screen.
 *
 * Renders nothing for [verification] of null, which means there is no attestation to report rather
 * than an origin that failed to verify.
 *
 * [reportUnverified] covers the case where the protocol could not confirm the origin at all. It
 * belongs on the connect screen, where the user is deciding whether to trust the dApp. On a signing
 * screen the origin was already accepted when the session was approved, so repeating it on every
 * signature would be noise that erodes the mismatch and scam warnings beside it.
 */
@Composable
fun VerificationAlert(verification: HSDAppVerification?, reportUnverified: Boolean = true) {
    if (verification == null) return

    val alert: Triple<AlertType, String, String>? = when {
        verification.isScam -> Triple(
            AlertType.Critical,
            stringResource(R.string.WalletConnect_Verify_Scam_Title),
            stringResource(R.string.WalletConnect_Verify_Scam)
        )

        verification.validation == HSDAppValidation.Invalid -> Triple(
            AlertType.Critical,
            stringResource(R.string.WalletConnect_Verify_Mismatch_Title),
            verification.origin
                ?.let { stringResource(R.string.WalletConnect_Verify_Mismatch, TextHelper.getCleanedUrl(it)) }
                ?: stringResource(R.string.WalletConnect_Verify_Mismatch_NoOrigin)
        )

        reportUnverified && verification.validation == HSDAppValidation.Unknown -> Triple(
            AlertType.Caution,
            stringResource(R.string.WalletConnect_Verify_Unknown_Title),
            stringResource(R.string.WalletConnect_Verify_Unknown)
        )

        else -> null
    }

    val (type, title, text) = alert ?: return

    AlertCard(
        modifier = Modifier.padding(horizontal = 16.dp),
        format = AlertFormat.Structured,
        type = type,
        titleCustom = title,
        text = text,
    )
    VSpacer(16.dp)
}
