package com.prafullk.upitracker.presentation.navigation

sealed class Route(val path: String) {
    object Onboarding : Route("onboarding")
    object Home : Route("home")
    object Transactions : Route("transactions")
    object TransactionDetail : Route("transaction/{id}") {
        fun create(id: String) = "transaction/$id"
    }
    object Entities : Route("entities")
    object EntityDetail : Route("entity/{id}") {
        fun create(id: String) = "entity/$id"
    }
    object Groups : Route("groups")
    object GroupDetail : Route("group/{id}") {
        fun create(id: String) = "group/$id"
    }
    object Analytics : Route("analytics")
    object Settings : Route("settings")
    object Classify : Route("classify")
    object AddEntity : Route("add_entity?transactionId={transactionId}")
}
