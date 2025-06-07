package cash.p.terminal.tangem.domain.task

import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.hdWallet.masterkey.AnyMasterKeyFactory
import com.tangem.operations.CommandResponse
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.operations.wallet.CreateWalletTask

class CreateWalletsResponse(
    val createWalletResponses: List<CreateWalletResponse>,
) : CommandResponse

class CreateWalletsTask(
    private val curves: List<EllipticCurve>,
    private val mnemonic: Mnemonic? = null,
    private val passphrase: String?,
) : CardSessionRunnable<CreateWalletsResponse> {

    private val createdWalletsResponses = mutableListOf<CreateWalletResponse>()

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletsResponse>) -> Unit
    ) {
        if (curves.isEmpty()) {
            callback(CompletionResult.Failure(TangemSdkError.WalletIsNotCreated()))
            return
        }

        val curve = curves[createdWalletsResponses.size]
        createWallet(curve, session, callback)
    }

    private fun createWallet(
        curve: EllipticCurve,
        session: CardSession,
        callback: (result: CompletionResult<CreateWalletsResponse>) -> Unit,
    ) {
        val extendedPrivateKey = mnemonic?.let {
            AnyMasterKeyFactory(mnemonic = it, passphrase = passphrase ?: "").makeMasterKey(curve)
        }
        CreateWalletTask(curve, extendedPrivateKey).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    createdWalletsResponses.add(result.data)
                    if (createdWalletsResponses.size == curves.size) {
                        callback(
                            CompletionResult.Success(
                                CreateWalletsResponse(
                                    createdWalletsResponses
                                )
                            )
                        )
                        return@run
                    }
                    createWallet(curves[createdWalletsResponses.size], session, callback)
                }

                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}
