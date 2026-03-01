package com.prafullk.upitracker.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.prafullk.upitracker.presentation.screens.analytics.AnalyticsScreen
import com.prafullk.upitracker.presentation.screens.classify.ClassifyScreen
import com.prafullk.upitracker.presentation.screens.entities.EntityDetailScreen
import com.prafullk.upitracker.presentation.screens.entities.EntityListScreen
import com.prafullk.upitracker.presentation.screens.groups.GroupDetailScreen
import com.prafullk.upitracker.presentation.screens.groups.GroupListScreen
import com.prafullk.upitracker.presentation.screens.home.HomeScreen
import com.prafullk.upitracker.presentation.screens.settings.SettingsScreen
import com.prafullk.upitracker.presentation.screens.transactions.TransactionDetailScreen
import com.prafullk.upitracker.presentation.screens.transactions.TransactionListScreen

data class BottomNavItem(val route: Route, val icon: ImageVector, val label: String)

val bottomNavItems =
        listOf(
                BottomNavItem(Route.Home, Icons.Default.Home, "Home"),
                BottomNavItem(Route.Transactions, Icons.Default.CreditCard, "Transactions"),
                BottomNavItem(Route.Entities, Icons.Default.Group, "People"),
                BottomNavItem(
                        Route.Groups,
                        Icons.Default.BarChart,
                        "Groups"
                ) // using Groups as alternative to Analytics for now
        )

@Composable
fun PayLensNavGraph(
        navController: NavHostController,
        startDestination: String = Route.Home.path,
        modifier: Modifier = Modifier
) {
    Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val isTopLevelRoute =
                        bottomNavItems.any { it.route.path == currentDestination?.route }

                if (isTopLevelRoute) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    selected =
                                            currentDestination?.hierarchy?.any {
                                                it.route == item.route.path
                                            } == true,
                                    onClick = {
                                        navController.navigate(item.route.path) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                            )
                        }
                    }
                }
            },
            modifier = modifier
    ) { innerPadding ->
        NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.Onboarding.path) {
                com.prafullk.upitracker.presentation.screens.onboarding.OnboardingScreen(
                        onNavigateToHome = {
                            navController.navigate(Route.Home.path) {
                                popUpTo(Route.Onboarding.path) { inclusive = true }
                            }
                        }
                )
            }
            composable(Route.Home.path) {
                HomeScreen(
                        onNavigateToAllTransactions = {
                            navController.navigate(Route.Transactions.path)
                        },
                        onNavigateToTransactionDetail = { id ->
                            navController.navigate(Route.TransactionDetail.create(id))
                        }
                )
            }
            composable(Route.Transactions.path) {
                TransactionListScreen(
                        onNavigateToTransactionDetail = { id ->
                            navController.navigate(Route.TransactionDetail.create(id))
                        },
                        onNavigateToClassify = { navController.navigate(Route.Classify.path) }
                )
            }
            composable(Route.TransactionDetail.path) { navBackStackEntry ->
                val id = navBackStackEntry.arguments?.getString("id") ?: return@composable
                TransactionDetailScreen(
                        transactionId = id,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToEntity = { entityId ->
                            navController.navigate(Route.EntityDetail.create(entityId))
                        }
                )
            }
            composable(Route.Entities.path) {
                EntityListScreen(
                        onNavigateToEntityDetail = { id ->
                            navController.navigate(Route.EntityDetail.create(id))
                        }
                )
            }
            composable(Route.EntityDetail.path) { navBackStackEntry ->
                val id = navBackStackEntry.arguments?.getString("id") ?: return@composable
                EntityDetailScreen(
                        entityId = id,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToTransactionDetail = { txId ->
                            navController.navigate(Route.TransactionDetail.create(txId))
                        }
                )
            }
            composable(Route.Groups.path) {
                GroupListScreen(
                        onNavigateToGroupDetail = { id ->
                            navController.navigate(Route.GroupDetail.create(id))
                        }
                )
            }
            composable(Route.GroupDetail.path) { navBackStackEntry ->
                val id = navBackStackEntry.arguments?.getString("id") ?: return@composable
                GroupDetailScreen(
                        groupId = id,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToTransactionDetail = { txId ->
                            navController.navigate(Route.TransactionDetail.create(txId))
                        }
                )
            }
            composable(Route.Analytics.path) { AnalyticsScreen() }
            composable(Route.Settings.path) {
                SettingsScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(Route.Classify.path) {
                ClassifyScreen(onNavigateBack = { navController.navigateUp() })
            }
        }
    }
}
