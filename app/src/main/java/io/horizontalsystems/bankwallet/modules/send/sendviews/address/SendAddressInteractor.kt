package io.horizontalsystems.bankwallet.modules.send.sendviews.address

import io.horizontalsystems.bankwallet.core.IClipboardManager

class SendAddressInteractor(private val textHelper: IClipboardManager): SendAddressModule.IInteractor {

    var delegate: SendAddressModule.IInteractorDelegate? = null

    override val addressFromClipboard: String?
        get() = textHelper.getCopiedText()

    override val clipboardHasPrimaryClip: Boolean
        get() = textHelper.hasPrimaryClip
}
