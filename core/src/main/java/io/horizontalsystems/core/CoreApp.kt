package io.horizontalsystems.core

import android.app.Application

abstract class CoreApp : Application() {

    companion object : ICoreApp {
        override lateinit var backgroundManager: BackgroundManager
        override lateinit var encryptionManager: IEncryptionManager
        override lateinit var systemInfoManager: ISystemInfoManager
        override lateinit var keyStoreManager: IKeyStoreManager
        override lateinit var keyProvider: IKeyProvider
        override lateinit var pinComponent: IPinComponent
        override lateinit var pinStorage: IPinStorage
        override lateinit var thirdKeyboardStorage: IThirdKeyboard

        override lateinit var instance: CoreApp
    }

    open val testMode = false
}
