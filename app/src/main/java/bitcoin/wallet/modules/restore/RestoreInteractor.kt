package bitcoin.wallet.modules.restore

class RestoreInteractor() : RestoreModule.IInteractor {

    var delegate: RestoreModule.IInteractorDelegate? = null

    override fun restore(words: List<String>) {
//        if (mnemonic.validateWords(words)) {
//            try {
////                blockchainManager.initNewWallet(words)
//                delegate?.didRestore()
//            } catch (e: UserNotAuthenticatedException) {
//                delegate?.didFailToRestore(e)
//            }
//        } else {
//            delegate?.didFailToRestore(Exception())
//        }
    }
}
