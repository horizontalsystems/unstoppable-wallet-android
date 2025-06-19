package cash.p.terminal.tangem.domain.sdk

import androidx.annotation.DrawableRes
import cash.p.terminal.core.R
import cash.p.terminal.tangem.domain.model.ProductType
import cash.p.terminal.tangem.domain.model.ScanResponse
import cash.p.terminal.tangem.domain.task.CreateProductWalletTask
import cash.p.terminal.tangem.domain.task.ResetBackupCardTask
import cash.p.terminal.tangem.domain.task.ResetToFactorySettingsTask
import cash.p.terminal.tangem.domain.task.ScanProductTask
import cash.p.terminal.tangem.domain.task.reponse.CreateProductWalletTaskResponse
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.entities.TokenQuery
import com.tangem.Log
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.UserCode
import com.tangem.common.UserCodeType
import com.tangem.common.authentication.AuthenticationManager
import com.tangem.common.authentication.keystore.KeystoreManager
import com.tangem.common.card.Card
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.core.UserCodeRequestPolicy
import com.tangem.common.doOnResult
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.services.secure.SecureStorage
import com.tangem.common.usersCode.UserCodeRepository
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.operations.ScanTask
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.pins.SetUserCodeCommand
import com.tangem.operations.preflightread.PreflightReadFilter
import com.tangem.operations.sign.SignHashResponse
import com.tangem.operations.sign.SignResponse
import com.tangem.operations.usersetttings.SetUserCodeRecoveryAllowedTask
import io.horizontalsystems.core.CoreApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class TangemSdkManager(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val accountManager: IAccountManager
) {

    private val awaitInitializationMutex = Mutex()
    internal var lastScanResponse: ScanResponse? = null
        private set

    internal val tangemSdk: TangemSdk
        get() = cardSdkConfigRepository.sdk

    private val userCodeRepository by lazy {
        UserCodeRepository(
            keystoreManager = tangemSdk.keystoreManager,
            secureStorage = tangemSdk.secureStorage,
        )
    }

    val canUseBiometry: Boolean
        get() = tangemSdk.authenticationManager.canAuthenticate || needEnrollBiometrics

    val needEnrollBiometrics: Boolean
        get() = tangemSdk.authenticationManager.needEnrollBiometrics

    val keystoreManager: KeystoreManager
        get() = tangemSdk.keystoreManager

    val secureStorage: SecureStorage
        get() = tangemSdk.secureStorage

    val userCodeRequestPolicy: UserCodeRequestPolicy
        get() = tangemSdk.config.userCodeRequestPolicy

    suspend fun checkNeedEnrollBiometrics(awaitInitialization: Boolean): Boolean {
        return try {
            needEnrollBiometrics
        } catch (e: TangemSdkError.AuthenticationNotInitialized) {
            Log.error {
                "Trying to access `needEnrollBiometrics` flag when authentication manager is not initialized: " +
                        if (awaitInitialization) "awaiting initialization" else "failing"
            }

            if (awaitInitialization) {
                awaitAuthenticationManagerInitialization().needEnrollBiometrics
            } else {
                throw e
            }
        }
    }

    suspend fun checkCanUseBiometry(awaitInitialization: Boolean): Boolean {
        return try {
            canUseBiometry
        } catch (e: TangemSdkError.AuthenticationNotInitialized) {
            Log.error {
                "Trying to access `canUseBiometry` flag when authentication manager is not initialized: " +
                        if (awaitInitialization) "awaiting initialization" else "failing"
            }

            if (awaitInitialization) {
                val manager = awaitAuthenticationManagerInitialization()

                manager.canAuthenticate || manager.needEnrollBiometrics
            } else {
                throw e
            }
        }
    }

    suspend fun scanProduct(
        cardId: String?,
        blockchainsToDerive: List<TokenQuery>,
        allowsRequestAccessCodeFromRepository: Boolean = false,
        message: Message? = null
    ): CompletionResult<ScanResponse> {
        return coroutineScope {
            runTaskAsyncReturnOnMain(
                runnable = ScanProductTask(
                    card = null,
                    blockchainsToDerive = blockchainsToDerive,
                    allowsRequestAccessCodeFromRepository = allowsRequestAccessCodeFromRepository,
                ),
                cardId = cardId,
                initialMessage = message
            ).also {
                if (it is CompletionResult.Success) {
                    lastScanResponse = it.data
                }
            }
        }
    }

    suspend fun sign(
        cardId: String?,
        hash: ByteArray,
        walletPublicKey: ByteArray,
        derivationPath: DerivationPath?,
        message: Message? = null
    ): CompletionResult<SignHashResponse> = suspendCancellableCoroutine { continuation ->
        tangemSdk.sign(
            hash = hash,
            walletPublicKey = walletPublicKey,
            cardId = cardId,
            derivationPath = derivationPath,
            initialMessage = message
        ) { result ->
            when (result) {
                is CompletionResult.Success ->
                    if (continuation.isActive) {
                        accountManager.updateSignedHashes(result.data.totalSignedHashes ?: 0)
                        continuation.resume(CompletionResult.Success(result.data))
                    }

                is CompletionResult.Failure ->
                    if (continuation.isActive) {
                        continuation.resume(CompletionResult.Failure(result.error))
                    }
            }
        }
    }

    suspend fun sign(
        cardId: String?,
        hashes: Array<ByteArray>,
        walletPublicKey: ByteArray,
        derivationPath: DerivationPath?,
        message: Message? = null
    ): CompletionResult<SignResponse> = suspendCancellableCoroutine { continuation ->
        tangemSdk.sign(
            hashes = hashes,
            walletPublicKey = walletPublicKey,
            cardId = cardId,
            derivationPath = derivationPath,
            initialMessage = message
        ) { result ->
            when (result) {
                is CompletionResult.Success ->
                    if (continuation.isActive) {
                        accountManager.updateSignedHashes(result.data.totalSignedHashes ?: 0)
                        continuation.resume(CompletionResult.Success(result.data))
                    }

                is CompletionResult.Failure ->
                    if (continuation.isActive) {
                        continuation.resume(CompletionResult.Failure(result.error))
                    }
            }
        }
    }

    suspend fun createProductWallet(
        scanResponse: ScanResponse,
        shouldReset: Boolean = false
    ): CompletionResult<CreateProductWalletTaskResponse> {
        tangemSdk.config.setupForProduct(
            if (scanResponse.productType == ProductType.Ring) {
                com.tangem.common.core.ProductType.RING
            } else {
                com.tangem.common.core.ProductType.CARD
            },
        )

        return runTaskAsync(
            runnable = CreateProductWalletTask(
                shouldReset = shouldReset,
            ),
            cardId = scanResponse.card.cardId,
            initialMessage = Message(CoreApp.instance.getString(R.string.initial_message_create_wallet_body)),
            iconScanRes = null,
            preflightReadFilter = null,
        ).doOnResult { tangemSdk.config.setupForProduct(com.tangem.common.core.ProductType.ANY) }
            .doOnSuccess { result ->
                lastScanResponse?.let {
                    lastScanResponse = it.copy(
                        card = result.card,
                        derivedKeys = result.derivedKeys,
                        primaryCard = result.primaryCard,
                    )
                }
            }
    }

    suspend fun derivePublicKeys(
        cardId: String?,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
        preflightReadFilter: PreflightReadFilter?,
    ): CompletionResult<DerivationTaskResponse> {
        return runTaskAsyncReturnOnMain(
            runnable = DeriveMultipleWalletPublicKeysTask(derivations),
            cardId = cardId,
            preflightReadFilter = preflightReadFilter,
        )
    }

    suspend fun deriveExtendedPublicKey(
        cardId: String?,
        walletPublicKey: ByteArray,
        derivation: DerivationPath,
    ): CompletionResult<ExtendedPublicKey> = withContext(Dispatchers.Main) {
        runTaskAsyncReturnOnMain(
            DeriveWalletPublicKeyTask(walletPublicKey, derivation),
            cardId,
        )
    }

    suspend fun resetToFactorySettings(
        cardId: String?,
        allowsRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<Pair<ByteArray?, Boolean>> {
        return runTaskAsyncReturnOnMain(
            runnable = ResetToFactorySettingsTask(
                allowsRequestAccessCodeFromRepository = allowsRequestAccessCodeFromRepository,
            ),
            cardId = cardId,
            initialMessage = Message(CoreApp.instance.getString(R.string.reset_to_factory_settings)),
        )
    }

    suspend fun resetBackupCard(
        cardNumber: Int,
        firstWalletPublicKey: ByteArray
    ): CompletionResult<Pair<ByteArray?, Boolean>> {
        return runTaskAsyncReturnOnMain(
            runnable = ResetBackupCardTask(firstWalletPublicKey),
            initialMessage = Message(
                CoreApp.instance.getString(
                    R.string.initial_message_reset_backup_card_header,
                    cardNumber.toString(),
                ),
            ),
        )
    }

    suspend fun saveAccessCode(accessCode: String, cardsIds: Set<String>): CompletionResult<Unit> {
        return userCodeRepository.save(
            cardsIds = cardsIds,
            userCode = UserCode(
                type = UserCodeType.AccessCode,
                stringValue = accessCode,
            ),
        )
    }

    suspend fun deleteSavedUserCodes(cardsIds: Set<String>): CompletionResult<Unit> {
        return userCodeRepository.delete(cardsIds.toSet())
    }

    suspend fun clearSavedUserCodes(): CompletionResult<Unit> {
        return userCodeRepository.clear()
    }

    /*
        suspend fun setPasscode(cardId: String?): CompletionResult<SuccessResponse> {
            return runTaskAsyncReturnOnMain(
                SetUserCodeCommand.changePasscode(null),
                cardId,
                initialMessage = Message(resources.getStringSafe(R.string.initial_message_change_passcode_body)),
            )
        }
    */

    suspend fun setAccessCode(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            SetUserCodeCommand.changeAccessCode(null),
            cardId,
            initialMessage = Message(CoreApp.instance.getString(R.string.initial_message_change_access_code_body)),
        )
    }

    suspend fun restoreAccessCode(cardId: String) =
        suspendCancellableCoroutine { continuation ->
            tangemSdk.restoreAccessCode(cardId) { result ->
                if (continuation.isActive) continuation.resume(result)
            }
        }

    suspend fun setAccessCodeRecoveryEnabled(
        cardId: String?,
        enabled: Boolean,
    ): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            SetUserCodeRecoveryAllowedTask(enabled),
            cardId,
            initialMessage = Message(CoreApp.instance.getString(R.string.initial_message_tap_header)),
        )
    }

    suspend fun scanCard(
        cardId: String?,
        allowRequestAccessCodeFromRepository: Boolean,
        message: Message? = null,
    ): CompletionResult<Card> = runTaskAsyncReturnOnMain(
        runnable = ScanTask(allowRequestAccessCodeFromRepository),
        cardId = cardId,
        initialMessage = message,
    )

    private suspend fun <T> runTaskAsync(
        runnable: CardSessionRunnable<T>,
        preflightReadFilter: PreflightReadFilter?,
        cardId: String? = null,
        initialMessage: Message? = null,
        accessCode: String? = null,
        @DrawableRes iconScanRes: Int? = null,
    ): CompletionResult<T> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            tangemSdk.startSessionWithRunnable(
                runnable = runnable,
                cardId = cardId,
                initialMessage = initialMessage,
                accessCode = accessCode,
                preflightReadFilter = preflightReadFilter,
                iconScanRes = iconScanRes,
            ) { result ->
                if (continuation.isActive) continuation.resume(result)
            }
        }
    }

    private suspend fun <T> runTaskAsyncReturnOnMain(
        runnable: CardSessionRunnable<T>,
        cardId: String? = null,
        initialMessage: Message? = null,
        preflightReadFilter: PreflightReadFilter? = null,
    ): CompletionResult<T> {
        val result = runTaskAsync(
            runnable = runnable,
            cardId = cardId,
            initialMessage = initialMessage,
            preflightReadFilter = preflightReadFilter,
        )
        return withContext(Dispatchers.Main) { result }
    }

    /*@Suppress("MagicNumber")
    fun changeDisplayedCardIdNumbersCount(scanResponse: ScanResponse?) {
        tangemSdk.config.cardIdDisplayFormat = when {
            scanResponse == null -> CardIdDisplayFormat.Full
            scanResponse.cardTypesResolver.isTangemTwins() -> CardIdDisplayFormat.LastLuhn(4)
            else -> CardIdDisplayFormat.Full
        }
    }*/

    /*fun setUserCodeRequestPolicy(policy: UserCodeRequestPolicy) {
        tangemSdk.config.userCodeRequestPolicy = policy
    }*/

    private suspend fun awaitAuthenticationManagerInitialization(): AuthenticationManager {
        return awaitInitializationMutex.withLock {
            var attemps = 0

            do {
                if (tangemSdk.authenticationManager.isInitialized) {
                    break
                } else {
                    if (attemps++ >= MAX_INITIALIZE_ATTEMPTS) {
                        error("Can't initialize authentication manager after $MAX_INITIALIZE_ATTEMPTS attempts")
                    } else {
                        delay(timeMillis = 200)
                    }
                }
            } while (true)

            tangemSdk.authenticationManager
        }
    }

    companion object {
        private const val MAX_INITIALIZE_ATTEMPTS = 10
    }
}