package cash.p.terminal.modules.pin.core

import io.reactivex.subjects.PublishSubject

class PinManager(private val pinDbStorage: PinDbStorage) {
    val pinSetSubject = PublishSubject.create<Unit>()

    val isPinSet: Boolean
        get() = pinDbStorage.isLastLevelPinSet()

    @Throws
    fun store(pin: String, level: Int) {
        pinDbStorage.store(pin, level)
        pinSetSubject.onNext(Unit)
    }

    fun getPinLevel(pin: String): Int? {
        return pinDbStorage.getLevel(pin)
    }

    fun getPinLevelLast(): Int {
        return pinDbStorage.getPinLevelLast()
    }

    fun disablePin(level: Int) {
        if (level < 0 ||
            level == PinLevels.SECURE_RESET ||
            PinLevels.isLogLoggingLevel(level) ||
            PinLevels.isDeleteContactsLevel(level)
        ) {
            pinDbStorage.deleteForLevel(level)
        } else {
            // For Level 0 and above (regular, duress) - clear current, delete higher levels
            pinDbStorage.clearPasscode(level)
            // Delete duress levels (level+1 to 9999) - excludes reserved levels
            pinDbStorage.deleteUserLevelsFromLevel(level + 1)
            // Delete SECURE_RESET PIN
            pinDbStorage.deleteForLevel(PinLevels.SECURE_RESET)
            // Delete DELETE_CONTACTS PIN only when disabling the regular app PIN flow
            if (level == 0) {
                pinDbStorage.deleteForLevel(PinLevels.DELETE_CONTACTS)
            }
            // Delete LOG_LOGGING PIN for duress level only (same constraint as getDuressLevel)
            val duressLevel = level + 1
            if (duressLevel < PinLevels.SECURE_RESET) {
                pinDbStorage.deleteForLevel(PinLevels.logLoggingLevelFor(duressLevel))
            }
        }
        pinSetSubject.onNext(Unit)
    }

    fun disableDuressPin(level: Int) {
        // Delete duress levels (level to 9999) - excludes reserved levels
        pinDbStorage.deleteUserLevelsFromLevel(level)
        // Delete all LOG_LOGGING PINs for this level and above
        pinDbStorage.deleteLogLoggingPinsFromLevel(level)
        pinSetSubject.onNext(Unit)
    }

    fun isPinSetForLevel(level: Int): Boolean {
        return pinDbStorage.isPinSetForLevel(level)
    }

    fun isUnique(pin: String, userLevel: Int): Boolean {
        val pinLevel = pinDbStorage.getLevel(pin) ?: return true

        return pinLevel == userLevel
    }

    fun getNextHiddenWalletLevel(): Int {
        return pinDbStorage.getNextHiddenWalletLevel()
    }

}
