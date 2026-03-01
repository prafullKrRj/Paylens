package com.prafullk.upitracker.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prafullk.upitracker.data.db.DatabaseSeeder
import com.prafullk.upitracker.data.db.PayLensDatabase
import com.prafullk.upitracker.data.detection.upi.UpiAppDiscoveryService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(androidContext(), PayLensDatabase::class.java, "paylens.db")
                .fallbackToDestructiveMigration()
                .addCallback(
                        object : RoomDatabase.Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                // We can't immediately get instances via Koin here natively within
                                // the callback synchronously
                                // A better approach is to invoke seed after building.
                            }
                        }
                )
                .build()
                .also {
                    // Trigger seeding right after build
                    DatabaseSeeder.seedDatabase(it.groupDao())
                }
    }

    single { get<PayLensDatabase>().transactionDao() }
    single { get<PayLensDatabase>().trackedEntityDao() }
    single { get<PayLensDatabase>().groupDao() }
    single { get<PayLensDatabase>().accessibilityLogDao() }
    single { get<PayLensDatabase>().smsLogDao() }
    single { get<PayLensDatabase>().upiAppDao() }

    single { UpiAppDiscoveryService(androidContext(), get()) }
}

