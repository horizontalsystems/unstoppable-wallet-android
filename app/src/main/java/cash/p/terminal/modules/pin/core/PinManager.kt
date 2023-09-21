package cash.p.terminal.modules.pin.core

import android.text.TextUtils
import io.horizontalsystems.core.IEncryptionManager
import io.horizontalsystems.core.IPinStorage
import io.reactivex.subjects.PublishSubject

class PinManager(
    private val encryptionManager: IEncryptionManager,
    private val pinStorage: IPinStorage
) {

    val pinSetSubject = PublishSubject.create<Unit>()

    val isPinSet: Boolean
        get() = !pins.lastOrNull().isNullOrBlank()

    private val pins: List<String>
        get() {
            val string = pinStorage.pin
            return if (string != null && !TextUtils.isEmpty(string)) {
                encryptionManager.decrypt(string).split(",")
            } else {
                listOf("")
            }
        }

    @Throws
    fun store(pin: String, level: Int) {
        val tmp = pins.toMutableList()
        tmp[level] = pin

        pinStorage.pin = encryptionManager.encrypt(tmp.joinToString(","))
        pinSetSubject.onNext(Unit)
    }

    fun getPinLevel(pin: String): Int? {
        val index = pins.indexOf(pin)
        if (index == -1) return null

        return index
    }

    fun getPinLevelLast(): Int {
        return pins.size - 1
    }

    fun clear(level: Int) {
        pinStorage.biometricAuthEnabled = false

        val tmp = pins.subList(0, level) + listOf("")

        pinStorage.pin = encryptionManager.encrypt(tmp.joinToString(","))
        pinSetSubject.onNext(Unit)
    }

}