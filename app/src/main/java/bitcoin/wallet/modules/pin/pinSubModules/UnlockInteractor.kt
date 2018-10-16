package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.ISettingsManager
import bitcoin.wallet.modules.pin.PinInteractor
import bitcoin.wallet.viewHelpers.DateHelper
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class UnlockInteractor(private val storage: ILocalStorage, private val settings: ISettingsManager, private val keystoreSafeExecute: IKeyStoreSafeExecute) : PinInteractor() {

    private val defaultAttemptsNumber = 5
    private val blockingTimeInMinutes = 5L

    override fun submit(pin: String) {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    if (storage.getPin() == pin) {
                        delegate?.onCorrectPinSubmitted()
                        settings.setUnlockAttemptsLeft(defaultAttemptsNumber)
                    } else {
                        onWrongPinSubmit()
                    }
                }
        )
    }

    private fun onWrongPinSubmit() {
        val attemptsLeft = settings.getUnlockAttemptsLeft()
        if (attemptsLeft > 0) {
            settings.setUnlockAttemptsLeft(attemptsLeft - 1)
            delegate?.showAttemptsLeftWarning(attemptsLeft)
        } else {
            val blockTillDate = DateHelper.minutesAfterNow(blockingTimeInMinutes.toInt())
            settings.setBlockTillDate(blockTillDate)
            settings.setUnlockAttemptsLeft(1)
            delegate?.blockScreen()
            unblockScreenAfter(blockingTimeInMinutes * 60)
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()

        if (settings.isFingerprintEnabled()) {
            delegate?.onFingerprintEnabled()
        }

        settings.getBlockTillDate()?.let { dateInMillis ->
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
