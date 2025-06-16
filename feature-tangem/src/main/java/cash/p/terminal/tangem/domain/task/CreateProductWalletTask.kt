package cash.p.terminal.tangem.domain.task

import cash.p.terminal.tangem.domain.TangemConfig
import cash.p.terminal.tangem.domain.card.CardConfig
import cash.p.terminal.tangem.domain.getDerivationStyle
import cash.p.terminal.tangem.domain.getSupportedCurves
import cash.p.terminal.tangem.domain.task.reponse.CreateProductWalletTaskResponse
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.backup.StartPrimaryCardLinkingCommand
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.operations.read.ReadWalletsListCommand
import com.tangem.operations.wallet.CreateWalletResponse

class CreateProductWalletTask(
    private val mnemonic: Mnemonic? = null,
    private val passphrase: String? = null,
    private val shouldReset: Boolean,
) : CardSessionRunnable<CreateProductWalletTaskResponse> {

    override val allowsRequestAccessCodeFromRepository: Boolean = false

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }
        val commandProcessor = CreateWalletTangemWallet(mnemonic, passphrase, shouldReset, card)

        commandProcessor.proceed(card, session) {
            when (it) {
                is CompletionResult.Success -> {
                    callback(CompletionResult.Success(it.data))
                }

                is CompletionResult.Failure -> callback(CompletionResult.Failure(it.error))
            }
        }
    }
}

/**
 * Uses for multiWallet 1st and 2nd
 */
private class CreateWalletTangemWallet(
    private val mnemonic: Mnemonic?,
    private val passphrase: String?,
    private val shouldReset: Boolean,
    card: Card,
) : ProductCommandProcessor<CreateProductWalletTaskResponse> {

    private var primaryCard: PrimaryCard? = null
    private val cardConfig = CardConfig.createConfig(card)

    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val walletsOnCard = card.wallets.map { it.curve }.toSet()
        if (walletsOnCard.isEmpty()) {
            createMultiWallet(card, session, callback)
        } else if (shouldReset) {
            resetCard(card, session, callback)
        } else {
            callback(CompletionResult.Failure(TangemSdkError.WalletAlreadyCreated()))
        }
    }

    private fun createMultiWallet(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        CreateWalletsTask(cardConfig.mandatoryCurves, mnemonic, passphrase).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    checkIfAllWalletsCreated(card, session, result.data, callback)
                }

                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun checkIfAllWalletsCreated(
        card: Card,
        session: CardSession,
        createResponse: CreateWalletsResponse,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        if (card.firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
            proceedWithCreatedWallets(
                card = card,
                createWalletResponses = createResponse.createWalletResponses.map { it },
                session = session,
                callback = callback,
            )
            return
        }

        val command = ReadWalletsListCommand()
        command.run(session) { response ->
            when (response) {
                is CompletionResult.Success -> {
                    val cardInitializationValidator =
                        CardInitializationValidator(cardConfig.mandatoryCurves)
                    if (cardInitializationValidator.validateWallets(response.data.wallets)) {
                        proceedWithCreatedWallets(
                            card = card,
                            createWalletResponses = createResponse.createWalletResponses.map { it },
                            session = session,
                            callback = callback,
                        )
                    } else {
                        callback(CompletionResult.Failure(TangemSdkError.WalletAlreadyCreated()))
                    }
                }

                is CompletionResult.Failure -> callback(CompletionResult.Failure(response.error))
            }
        }
    }

    private fun resetCard(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val resetCommand = ResetToFactorySettingsTask(allowsRequestAccessCodeFromRepository = false)
        resetCommand.run(session) {
            when (it) {
                is CompletionResult.Success -> {
                    createMultiWallet(card, session, callback)
                }

                is CompletionResult.Failure -> callback(CompletionResult.Failure(it.error))
            }
        }
    }

    private fun proceedWithCreatedWallets(
        card: Card,
        createWalletResponses: List<CreateWalletResponse>,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        when {
            card.settings.isBackupAllowed -> {
                linkPrimaryCard(card, createWalletResponses, session, callback)
            }

            card.settings.isHDWalletAllowed -> {
                deriveKeys(card, createWalletResponses, session, callback)
            }

            else -> {
                callback(
                    CompletionResult.Success(
                        CreateProductWalletTaskResponse(card = session.environment.card!!),
                    ),
                )
            }
        }
    }

    private fun linkPrimaryCard(
        card: Card,
        createWalletResponse: List<CreateWalletResponse>,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        StartPrimaryCardLinkingCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    primaryCard = result.data
                    when {
                        card.settings.isHDWalletAllowed -> {
                            deriveKeys(card, createWalletResponse, session, callback)
                        }

                        else -> {
                            callback(
                                CompletionResult.Success(
                                    CreateProductWalletTaskResponse(
                                        card = session.environment.card!!,
                                        primaryCard = primaryCard,
                                    ),
                                ),
                            )
                        }
                    }
                }

                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun deriveKeys(
        card: Card,
        createWalletResponse: List<CreateWalletResponse>,
        session: CardSession,
        callback: (result: CompletionResult<CreateProductWalletTaskResponse>) -> Unit,
    ) {
        val map = mutableMapOf<ByteArrayKey, List<DerivationPath>>()


        createWalletResponse.forEach { response ->
            val derivationPaths = TangemConfig.getDefaultTokens.filter {
                it.blockchainType.getSupportedCurves().contains(response.wallet.curve)
            }.mapNotNull {
                card.getDerivationStyle()?.getConfig()
                    ?.derivations(it.blockchainType, "")?.values?.firstOrNull()
            }
            if (derivationPaths.isNotEmpty()) {
                map[response.wallet.publicKey.toMapKey()] = derivationPaths
            }
        }
        val cardEnv = session.environment.card
        if (cardEnv == null) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }
        if (map.isEmpty()) {
            // if there is no blockchains to derive, just return success response with empty derivedKeys
            callback(
                CompletionResult.Success(
                    CreateProductWalletTaskResponse(
                        card = cardEnv,
                        primaryCard = primaryCard,
                    ),
                ),
            )
            return
        }

        DeriveMultipleWalletPublicKeysTask(map)
            .run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        callback(
                            CompletionResult.Success(
                                CreateProductWalletTaskResponse(
                                    card = cardEnv,
                                    derivedKeys = result.data.entries,
                                    primaryCard = primaryCard,
                                ),
                            ),
                        )
                    }

                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
    }
}