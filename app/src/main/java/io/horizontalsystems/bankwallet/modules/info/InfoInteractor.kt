package io.horizontalsystems.bankwallet.modules.info

import io.horizontalsystems.bankwallet.core.IClipboardManager

class InfoInteractor(private var clipboardManager: IClipboardManager) : InfoModule.IInteractor {

    override fun onCopy(value: String) {
        clipboardManager.copyText(value)
    }

}
