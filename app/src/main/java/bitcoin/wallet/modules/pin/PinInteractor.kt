package bitcoin.wallet.modules.pin

abstract class PinInteractor : PinModule.IInteractor {

    var delegate: PinModule.IInteractorDelegate? = null

}
