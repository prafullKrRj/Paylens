package com.prafullk.upitracker.di

import com.prafullk.upitracker.presentation.screens.analytics.AnalyticsViewModel
import com.prafullk.upitracker.presentation.screens.classify.ClassifyViewModel
import com.prafullk.upitracker.presentation.screens.entities.EntityDetailViewModel
import com.prafullk.upitracker.presentation.screens.entities.EntityViewModel
import com.prafullk.upitracker.presentation.screens.groups.GroupDetailViewModel
import com.prafullk.upitracker.presentation.screens.groups.GroupViewModel
import com.prafullk.upitracker.presentation.screens.home.HomeViewModel
import com.prafullk.upitracker.presentation.screens.onboarding.OnboardingViewModel
import com.prafullk.upitracker.presentation.screens.settings.SettingsViewModel
import com.prafullk.upitracker.presentation.screens.transactions.TransactionDetailViewModel
import com.prafullk.upitracker.presentation.screens.transactions.TransactionViewModel
import com.prafullk.upitracker.presentation.screens.upiapps.UpiAppsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { TransactionViewModel(get(), get()) }
    viewModel { (transactionId: String) ->
        TransactionDetailViewModel(transactionId, get(), get(), get())
    }
    viewModel { EntityViewModel(get()) }
    viewModel { (entityId: String) -> EntityDetailViewModel(entityId, get(), get(), get()) }
    viewModel { GroupViewModel(get(), get()) }
    viewModel { (groupId: String) -> GroupDetailViewModel(groupId, get(), get()) }
    viewModel { AnalyticsViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { ClassifyViewModel(get(), get(), get(), get()) }
    viewModel { UpiAppsViewModel(get(), get()) }
}
