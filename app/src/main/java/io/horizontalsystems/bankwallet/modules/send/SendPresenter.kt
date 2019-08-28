package io.horizontalsystems.bankwallet.modules.send

class SendPresenter(private val interactor: SendModule.ISendInteractor,
                    private val router: SendModule.IRouter,
                    override val handler: SendModule.ISendHandler) :
        SendModule.IViewDelegate, SendModule.ISendInteractorDelegate, SendModule.ISendHandlerDelegate {

    override lateinit var view: SendModule.IView

    // SendModule.IViewDelegate

    override fun onViewDidLoad() {
        view.loadInputItems(handler.inputItems)
    }

    override fun onModulesDidLoad() {
        handler.onModulesDidLoad()
    }

    override fun onAddressScan(address: String) {
        handler.onAddressScan(address)
    }

    override fun onProceedClicked() {
        view.showConfirmation(handler.confirmationViewItems())
    }

    override fun onSendConfirmed(memo: String?) {
        interactor.send(handler.sendSingle())
    }

    override fun onClear() {
        interactor.clear()
    }

    // SendModule.ISendInteractorDelegate

    override fun didSend() {
        router.closeWithSuccess()
    }

    override fun didFailToSend(error: Throwable) {
        view.showErrorInToast(error)
    }

    // SendModule.ISendHandlerDelegate

    override fun onChange(isValid: Boolean) {
        view.setSendButtonEnabled(isValid)
    }

}
