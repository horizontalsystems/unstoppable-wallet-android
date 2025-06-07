package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.sdk.TangemSdkManager
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.crypto.NetworkType
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class GetExtendedPublicKeyForCurrencyUseCase(
    private val tangemSdkManager: TangemSdkManager
) {
    suspend operator fun invoke(
        derivationPath: DerivationPath,
        extendedPublicKey: ExtendedPublicKey,
        isBip44DerivationStyleXPUB: Boolean,
    ): String {
        var childKey = makeChildKey(
            isBip44DerivationStyleXPUB = isBip44DerivationStyleXPUB,
            extendedPublicKey = extendedPublicKey,
            derivationPath = derivationPath,
        )

        var parentKey = Key(
            derivationPath = childKey.derivationPath.dropLastNodes(1),
            extendedPublicKey = null,
        )

        val pendingDerivations = getPendingDerivations(childKey, parentKey)
        val derivedKeys = deriveKeys(
            seedKey = extendedPublicKey.publicKey,
            paths = pendingDerivations,
        )

        if (childKey.extendedPublicKey == null) {
            childKey = childKey.copy(
                extendedPublicKey = derivedKeys[childKey.derivationPath]
                    ?: error("Failed to derive child key"),
            )
        }

        if (parentKey.extendedPublicKey == null) {
            parentKey = parentKey.copy(
                extendedPublicKey = derivedKeys[parentKey.derivationPath]
                    ?: error("Failed to derive parent key"),
            )
        }

        return makeExtendedKey(childKey, parentKey)
    }

    private suspend fun deriveKeys(
        seedKey: ByteArray,
        paths: MutableList<DerivationPath>,
    ): ExtendedPublicKeysMap = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            launch {
                tangemSdkManager.derivePublicKeys(
                    cardId = null,
                    derivations = mapOf(ByteArrayKey(seedKey) to paths),
                    preflightReadFilter = null
                ).doOnSuccess {
                    if (continuation.isActive) continuation.resume(
                        it.entries.getValue(
                            ByteArrayKey(
                                seedKey
                            )
                        )
                    )
                }.doOnFailure {
                    throw it
                }
            }
        }
    }

    private fun makeExtendedKey(childKey: Key, parentKey: Key): String {
        val publicKey = childKey.extendedPublicKey?.publicKey ?: error("No public key found")
        val chainCode = childKey.extendedPublicKey.chainCode
        val lastChildNode = childKey.derivationPath.nodes.last()
        val parentPublicKey = parentKey.extendedPublicKey?.publicKey

        val depth = childKey.derivationPath.nodes.size
        val childNumber = lastChildNode.index
        val parentFingerprint = parentPublicKey
            ?.calculateSha256()?.calculateRipemd160()
            ?.take(PARENT_FINGERPRINT_SIZE)?.toByteArray()
            ?: error("No parent fingerprint found")

        return ExtendedPublicKey(
            publicKey = publicKey,
            chainCode = chainCode,
            depth = depth,
            parentFingerprint = parentFingerprint,
            childNumber = childNumber,
        ).serialize(NetworkType.Mainnet)
    }

    private fun getPendingDerivations(childKey: Key, parentKey: Key): MutableList<DerivationPath> {
        val pendingDerivations = mutableListOf<DerivationPath>()

        if (childKey.extendedPublicKey == null) {
            pendingDerivations.add(childKey.derivationPath)
        }

        if (parentKey.extendedPublicKey == null) {
            pendingDerivations.add(parentKey.derivationPath)
        }

        return pendingDerivations
    }

    private fun makeChildKey(
        isBip44DerivationStyleXPUB: Boolean,
        extendedPublicKey: ExtendedPublicKey,
        derivationPath: DerivationPath,
    ): Key = if (isBip44DerivationStyleXPUB) {
        Key(derivationPath.dropLastNodes(2), null)
    } else {
        Key(derivationPath, extendedPublicKey)
    }

    private fun DerivationPath.dropLastNodes(count: Int): DerivationPath {
        return DerivationPath(nodes.dropLast(count))
    }

    private data class Key(
        val derivationPath: DerivationPath,
        val extendedPublicKey: ExtendedPublicKey?,
    )

    private companion object {
        const val PARENT_FINGERPRINT_SIZE = 4
    }
}
