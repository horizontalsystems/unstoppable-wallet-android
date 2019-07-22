package io.horizontalsystems.bankwallet.modules.restore.eos

class RestoreEosInteractor : RestoreEosModule.IInteractor {

    var delegate: RestoreEosModule.IInteractorDelegate? = null

    override fun validate(accountName: String, privateKey: String) {
        delegate?.onValidationSuccess(accountName, privateKey)
    }
}
