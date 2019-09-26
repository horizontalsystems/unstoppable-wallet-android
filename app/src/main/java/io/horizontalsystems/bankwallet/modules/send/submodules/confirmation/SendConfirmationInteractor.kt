package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import io.horizontalsystems.bankwallet.core.IClipboardManager


class SendConfirmationInteractor(private val clipboardManager: IClipboardManager)
    : SendConfirmationModule.Interactor {

    var delegate: SendConfirmationModule.InteractorDelegate? = null

    override fun copyToClipboard(coinAddress: String) {
        clipboardManager.copyText(coinAddress)
        delegate?.didCopyToClipboard()
    }

}
