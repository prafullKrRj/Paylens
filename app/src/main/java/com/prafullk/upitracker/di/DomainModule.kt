package com.prafullk.upitracker.di

import com.prafullk.upitracker.domain.usecase.entity.CreateEntityUseCase
import com.prafullk.upitracker.domain.usecase.entity.MergeEntitiesUseCase
import com.prafullk.upitracker.domain.usecase.entity.ObserveEntitiesUseCase
import com.prafullk.upitracker.domain.usecase.group.ManageGroupUseCase
import com.prafullk.upitracker.domain.usecase.group.ObserveGroupsUseCase
import com.prafullk.upitracker.domain.usecase.transaction.ClassifyTransactionUseCase
import com.prafullk.upitracker.domain.usecase.transaction.GetSpendingAnalyticsUseCase
import com.prafullk.upitracker.domain.usecase.transaction.LogTransactionUseCase
import com.prafullk.upitracker.domain.usecase.transaction.ObserveTransactionsUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { LogTransactionUseCase(get(), get(), get(), get()) }
    factory { ObserveTransactionsUseCase(get()) }
    factory { ClassifyTransactionUseCase(get()) }
    factory { GetSpendingAnalyticsUseCase(get()) }

    factory { ObserveEntitiesUseCase(get()) }
    factory { CreateEntityUseCase(get()) }
    factory { MergeEntitiesUseCase(get()) }

    factory { ObserveGroupsUseCase(get()) }
    factory { ManageGroupUseCase(get()) }
}
