package io.horizontalsystems.bankwallet.modules.settings.security

class SecuritySettingsPresenter(private val router: SecuritySettingsModule.ISecuritySettingsRouter, private val interactor: SecuritySettingsModule.ISecuritySettingsInteractor)
    : SecuritySettingsModule.ISecuritySettingsViewDelegate, SecuritySettingsModule.ISecuritySettingsInteractorDelegate {

    var view: SecuritySettingsModule.ISecuritySettingsView? = null

    override fun viewDidLoad() {
        view?.setBackedUp(interactor.allBackedUp)
        view?.setPinEnabled(interactor.isPinEnabled)

        if (interactor.hasFingerprintSensor && interactor.isPinEnabled) {
            view?.showFingerprintSettings(interactor.isFingerPrintEnabled)
        } else {
            view?.hideFingerprintSettings()
        }
    }

    override fun didTapManageKeys() {
        router.showManageKeys()
    }

    override fun didTapEnablePin(enable: Boolean) {
        if (enable) {
            router.showSetPin()
        } else {
            router.showUnlockPin()
        }
    }

    override fun didTapEditPin() {
        router.showEditPin()
    }

    override fun didTapEnableFingerprint(enable: Boolean) {
        if (interactor.hasEnrolledFingerprints) {
            interactor.isFingerPrintEnabled = enable
        } else {
            interactor.isFingerPrintEnabled = false

            view?.showNoEnrolledFingerprints()
            view?.showFingerprintSettings(false)
        }
    }

    override fun didSetPin() {
        updateViews()
    }

    override fun didCancelSetPin() {
        updateViews()
    }

    override fun didUnlockPinToDisablePin() {
        interactor.disablePin()

        updateViews()
    }

    override fun didCancelUnlockPinToDisablePin() {
        updateViews()
    }

    override fun onClear() {
        interactor.clear()
    }

    private fun updateViews() {
        view?.setPinEnabled(interactor.isPinEnabled)
        if (interactor.isPinEnabled) {
            view?.showFingerprintSettings(enabled = interactor.isFingerPrintEnabled)
        } else {
            view?.hideFingerprintSettings()
        }
    }

    // ISecuritySettingsInteractorDelegate

    override fun didAllBackedUp(allBackedUp: Boolean) {
        view?.setBackedUp(allBackedUp)
    }

}
