package io.horizontalsystems.dapp.core

/**
 * Origin attestation reported by the dApp protocol.
 *
 * Everything else the wallet shows about a dApp — name, icons, url — is self-reported by that dApp
 * and can be set to anything, so a phishing site can claim to be a well-known one. This is the
 * attested counterpart: [origin] is where the connection was actually observed to come from, and
 * [validation] says whether that matches the domain the dApp claims.
 */
data class HSDAppVerification(
    val validation: HSDAppValidation,
    val origin: String?,
    val isScam: Boolean,
) {
    companion object {
        /**
         * Used where the protocol supplies no attestation. Callers treat it as "not verified"
         * rather than "verified fine", so a missing signal never reads as trust.
         */
        val Unknown = HSDAppVerification(
            validation = HSDAppValidation.Unknown,
            origin = null,
            isScam = false,
        )
    }
}

enum class HSDAppValidation {
    /** Attested origin matches the domain the dApp claims. */
    Valid,

    /** Attested origin contradicts the claimed domain — the dApp is impersonating another. */
    Invalid,

    /** Verification could not be performed; the claim is neither confirmed nor contradicted. */
    Unknown,
}
