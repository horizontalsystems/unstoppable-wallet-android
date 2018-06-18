package bitcoin.wallet.core

import android.app.Application
import android.arch.persistence.room.Room
import android.content.SharedPreferences
import android.preference.PreferenceManager
import bitcoin.wallet.core.database.AppDatabase

class App : Application() {

    companion object {
        lateinit var preferences: SharedPreferences
        lateinit var db: AppDatabase

        val testMode = true
    }

    override fun onCreate() {
        super.onCreate()

        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        db = Room
                .databaseBuilder(applicationContext, AppDatabase::class.java, "database-name")
                .allowMainThreadQueries()
                .build()
    }
}