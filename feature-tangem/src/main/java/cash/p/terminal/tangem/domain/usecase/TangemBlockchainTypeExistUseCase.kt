package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.wallet.IHardwarePublicKeyStorage
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.HardwarePublicKey

class TangemBlockchainTypeExistUseCase(private val hardwarePublicKeyStorage: IHardwarePublicKeyStorage) {

    private var existedPublicKeys: List<HardwarePublicKey>? = null

    suspend fun loadKeys(accountId: String) {
        existedPublicKeys = hardwarePublicKeyStorage.getAllPublicKeys(accountId)
    }

    operator fun invoke(token: Token): Boolean {
        return existedPublicKeys?.any {
            it.blockchainType == token.blockchainType.uid && it.tokenType == token.type
        } == true
    }
}