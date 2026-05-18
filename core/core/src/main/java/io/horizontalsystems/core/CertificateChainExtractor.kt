package io.horizontalsystems.core

import java.security.MessageDigest
import java.security.cert.CertPathValidatorException
import java.security.cert.X509Certificate

private const val MAX_CERTS_REPORTED = 6
private const val MAX_SANS_PER_CERT = 8
private const val MAX_SAN_VALUE_LENGTH = 96

/**
 * Walks the cause chain looking for [CertPathValidatorException] and extracts
 * diagnostic information about the certificates the server presented.
 *
 * Useful when an SSL handshake fails (`Trust anchor for certification path
 * not found`) — the resulting fields help distinguish between local AV/proxy
 * MITM, corporate CA injection, and genuine server-side issues.
 *
 * Returns an empty map for non-SSL errors or when the trust manager did not
 * propagate the chain. Best-effort: any extraction failure is swallowed so
 * diagnostics never break the calling error path.
 */
fun Throwable.extractCertificateChainInfo(): Map<String, String> = tryOrEmpty {
    val certPathException = findCauseOfType<CertPathValidatorException>() ?: return@tryOrEmpty emptyMap()
    val certificates = certPathException.certPath
        ?.certificates
        ?.filterIsInstance<X509Certificate>()
        ?.takeIf { it.isNotEmpty() }
        ?: return@tryOrEmpty emptyMap()

    buildMap {
        tryOrNullLocal { certPathException.index }?.takeIf { it >= 0 }?.let { idx ->
            put("Cert Failed Index", idx.toString())
        }
        certificates.take(MAX_CERTS_REPORTED).forEachIndexed { index, cert ->
            putAll(cert.toDiagnosticEntries(index))
        }
    }
}

private fun X509Certificate.toDiagnosticEntries(index: Int): Map<String, String> = buildMap {
    putIfPresent("Cert[$index] Subject") { subjectX500Principal.name }
    putIfPresent("Cert[$index] Issuer") { issuerX500Principal.name }
    putIfPresent("Cert[$index] Serial") { serialNumber.toString(16) }
    putIfPresent("Cert[$index] Valid From") { notBefore.toString() }
    putIfPresent("Cert[$index] Valid Until") { notAfter.toString() }
    putIfPresent("Cert[$index] SHA-256") { sha256Fingerprint() }
    putIfPresent("Cert[$index] SANs") { subjectAltNamesSummary() }
}

private inline fun MutableMap<String, String>.putIfPresent(key: String, valueProducer: () -> String?) {
    val value = tryOrNullLocal(valueProducer)?.takeIf { it.isNotBlank() } ?: return
    put(key, value)
}

private inline fun <reified T : Throwable> Throwable.findCauseOfType(): T? {
    val visited = mutableSetOf<Throwable>()
    var current: Throwable? = this
    while (current != null && visited.add(current)) {
        if (current is T) return current
        current = current.cause
    }
    return null
}

private fun X509Certificate.sha256Fingerprint(): String =
    MessageDigest.getInstance("SHA-256").digest(encoded)
        .joinToString(":") { "%02X".format(it) }

private fun X509Certificate.subjectAltNamesSummary(): String? {
    val all = subjectAlternativeNames ?: return null
    val values = all.mapNotNull { entry ->
        entry.getOrNull(1)?.toString()?.take(MAX_SAN_VALUE_LENGTH)
    }
    if (values.isEmpty()) return null
    val shown = values.take(MAX_SANS_PER_CERT)
    val omitted = values.size - shown.size
    return if (omitted > 0) {
        shown.joinToString(", ") + ", +$omitted more"
    } else {
        shown.joinToString(", ")
    }
}

private inline fun tryOrEmpty(block: () -> Map<String, String>): Map<String, String> = try {
    block()
} catch (_: Throwable) {
    emptyMap()
}

private inline fun <T> tryOrNullLocal(block: () -> T?): T? = try {
    block()
} catch (_: Throwable) {
    null
}
