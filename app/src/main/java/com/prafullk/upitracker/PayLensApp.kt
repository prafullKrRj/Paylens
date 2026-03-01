package com.prafullk.upitracker

import android.app.Application
import com.prafullk.upitracker.di.appModule
import com.prafullk.upitracker.di.dataModule
import com.prafullk.upitracker.di.domainModule
import com.prafullk.upitracker.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PayLensApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PayLensApp)
            modules(appModule, dataModule, domainModule, presentationModule)
        }
    }
}
