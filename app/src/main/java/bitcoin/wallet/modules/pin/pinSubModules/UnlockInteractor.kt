package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.ISecuredStorage
import bitcoin.wallet.modules.pin.PinInteractor
import bitcoin.wallet.viewHelpers.DateHelper
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class UnlockInteractor(private val storage: ILocalStorage, private val secureStorage: ISecuredStorage, private val keystoreSafeExecute: IKeyStoreSafeExecute) : PinInteractor() {

    private val defaultAttemptsNumber = 5
    private val blockingTimeInMinutes = 5L

    override fun submit(pin: String) {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    if (secureStorage.savedPin == pin) {
                        delegate?.onCorrectPinSubmitted()
                        storage.unlockAttemptsLeft = defaultAttemptsNumber
                    } else {
                        onWrongPinSubmit()
                    }
                }
        )
    }

    private fun onWrongPinSubmit() {
        val attemptsLeft = storage.unlockAttemptsLeft
        if (attemptsLeft > 0) {
            storage.unlockAttemptsLeft = attemptsLeft - 1
            delegate?.onWrongPin()
        } else {
            val blockTillDate = DateHelper.minutesAfterNow(blockingTimeInMinutes.toInt())
            storage.blockTillDate = blockTillDate
            storage.unlockAttemptsLeft = 1
            delegate?.blockScreen()
            unblockScreenAfter(blockingTimeInMinutes * 60)
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()

        if (storage.isBiometricOn) {
            delegate?.onFingerprintEnabled()
        }

        storage.blockTillDate?.let { dateInMillis ->
            val secondsAgo = DateHelper.getSecondsAgo(dateInMillis)
            if (secondsAgo < 0) {
                delegate?.blockScreen()
                unblockScreenAfter(secondsAgo)
            }
        }
    }

    private fun unblockScreenAfter(seconds: Long) {
        val disposable = Completable.timer(seconds, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe {
                    delegate?.unblockScreen()
                }
    }

    override fun onBackPressed() {
        delegate?.onMinimizeApp()
    }

}
