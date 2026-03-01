package com.prafullk.upitracker.di

import com.prafullk.upitracker.data.detection.accessibility.NodeTreeParser
import com.prafullk.upitracker.data.detection.sms.SmsParser
import com.prafullk.upitracker.data.intelligence.DeduplicationEngine
import com.prafullk.upitracker.data.repository.EntityRepositoryImpl
import com.prafullk.upitracker.data.repository.GroupRepositoryImpl
import com.prafullk.upitracker.data.repository.TransactionRepositoryImpl
import com.prafullk.upitracker.domain.repository.EntityRepository
import com.prafullk.upitracker.domain.repository.GroupRepository
import com.prafullk.upitracker.domain.repository.TransactionRepository
import org.koin.dsl.module

val dataModule = module {
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    single<EntityRepository> { EntityRepositoryImpl(get()) }
    single<GroupRepository> { GroupRepositoryImpl(get()) }

    single { DeduplicationEngine(get(), get()) }
    single { NodeTreeParser(get()) }
    single { SmsParser }

    single { com.prafullk.upitracker.data.intelligence.EntityMatcher(get()) }
    single { com.prafullk.upitracker.data.intelligence.TransactionClassifier(get(), get()) }
}
