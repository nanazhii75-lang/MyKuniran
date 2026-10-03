package com.kuniran.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kuniran.R
import com.kuniran.feature.agenda.CalendarRsvpScreen
import com.kuniran.feature.agenda.CalendarRsvpViewModel
import com.kuniran.feature.attendance.AttendanceViewModel
import com.kuniran.feature.attendance.QrAttendanceScannerScreen
import com.kuniran.feature.auth.AuthViewModel
import com.kuniran.feature.auth.CreateRtScreen
import com.kuniran.feature.auth.JoinOrCreateRtScreen
import com.kuniran.feature.auth.JoinRtScreen
import com.kuniran.feature.auth.LoginScreen
import com.kuniran.feature.finance.FinanceDashboardScreen
import com.kuniran.feature.finance.FinanceRecordViewModel
import com.kuniran.feature.finance.FinanceScreen
import com.kuniran.feature.finance.FinanceViewModel
import com.kuniran.feature.forum.DiscussionForumScreen
import com.kuniran.feature.forum.ForumViewModel
import com.kuniran.feature.help.HelpAndFaqScreen
import com.kuniran.feature.home.HomeScreen
import com.kuniran.feature.home.HomeViewModel
import com.kuniran.feature.members.MembersScreen
import com.kuniran.feature.members.MembersViewModel
import com.kuniran.feature.members.ResidentDirectoryScreen
import com.kuniran.core.fcm.FcmManager
import androidx.compose.ui.platform.LocalContext
import com.kuniran.feature.profile.CompleteProfileScreen
import com.kuniran.feature.profile.CompleteProfileViewModel
import com.kuniran.feature.members.ResidentDirectoryViewModel
import com.kuniran.feature.settings.SettingsScreen
import com.kuniran.feature.settings.SettingsViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object CompleteProfile : Screen("complete_profile")
    object ChooseRt : Screen("choose_rt")
    object JoinRt : Screen("join_rt")
    object CreateRt : Screen("create_rt")
    object HelpFaq : Screen("help_faq")
    object FinanceDashboard : Screen("finance_dashboard")
    object ResidentDirectory : Screen("resident_directory")
    object CalendarRsvp : Screen("calendar_rsvp")
    object Forum : Screen("forum")
    object QrScanner : Screen("qr_scanner")
    object Main : Screen("main")
}

sealed class BottomNavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavTab("home", R.string.nav_home, Icons.Filled.Campaign, Icons.Outlined.Campaign)
    object Members : BottomNavTab("members", R.string.nav_members, Icons.Filled.Group, Icons.Outlined.Group)
    object Finance : BottomNavTab("finance", R.string.nav_finance, Icons.Filled.Payments, Icons.Outlined.Payments)
    object Settings : BottomNavTab("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun NavigationRoot(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val rootNavController = rememberNavController()
    val appContext = LocalContext.current.applicationContext
    val sessionUserId by container.sessionManager.currentUserId.collectAsState()

    // Token notifikasi dipasangkan ke akun yang sedang login. Tanpa ini token baru terdaftar
    // saat aplikasi dibuka ulang, sehingga setelah ganti akun notifikasi bisa masuk ke akun lama.
    LaunchedEffect(sessionUserId) {
        if (!sessionUserId.isNullOrBlank()) {
            FcmManager.registerCurrentToken(appContext)
        }
    }

    val startDestination = if (container.sessionManager.isLoggedIn()) {
        if (!container.sessionManager.isProfileComplete()) Screen.CompleteProfile.route
        else if (!container.sessionManager.getRtId().isNullOrBlank()) Screen.Main.route
        else Screen.ChooseRt.route
    } else {
        Screen.Login.route
    }

    LaunchedEffect(Unit) {
        container.sessionManager.sessionExpiredFlow.collect {
            rootNavController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = rootNavController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel {
                AuthViewModel(
                    container.getCurrentUserUseCase,
                    container.signInWithGoogleUseCase,
                    container.signOutUseCase,
                    container.createRtUseCase,
                    container.previewRtUseCase,
                    container.requestJoinRtUseCase,
                    container.cancelJoinRequestUseCase,
                    container.checkUsernameAvailableUseCase
                )
            }
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { hasRt ->
                    if (!container.sessionManager.isProfileComplete()) {
                        rootNavController.navigate(Screen.CompleteProfile.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else if (hasRt) {
                        rootNavController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        rootNavController.navigate(Screen.ChooseRt.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.CompleteProfile.route) {
            val profileViewModel: CompleteProfileViewModel = viewModel {
                CompleteProfileViewModel(
                    container.getCurrentUserUseCase,
                    container.updateProfileUseCase,
                    container.signOutUseCase
                )
            }
            CompleteProfileScreen(
                viewModel = profileViewModel,
                onDone = {
                    val next = if (container.sessionManager.getRtId().isNullOrBlank())
                        Screen.ChooseRt.route else Screen.Main.route
                    rootNavController.navigate(next) {
                        popUpTo(Screen.CompleteProfile.route) { inclusive = true }
                    }
                },
                onSignedOut = {
                    rootNavController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ChooseRt.route) {
            JoinOrCreateRtScreen(
                onNavigateJoin = { rootNavController.navigate(Screen.JoinRt.route) },
                onNavigateCreate = { rootNavController.navigate(Screen.CreateRt.route) }
            )
        }

        composable(Screen.JoinRt.route) {
            val authViewModel: AuthViewModel = viewModel {
                AuthViewModel(
                    container.getCurrentUserUseCase,
                    container.signInWithGoogleUseCase,
                    container.signOutUseCase,
                    container.createRtUseCase,
                    container.previewRtUseCase,
                    container.requestJoinRtUseCase,
                    container.cancelJoinRequestUseCase,
                    container.checkUsernameAvailableUseCase
                )
            }
            JoinRtScreen(
                viewModel = authViewModel,
                onBack = { rootNavController.popBackStack() },
                onJoinApproved = {
                    rootNavController.navigate(Screen.Main.route) {
                        popUpTo(Screen.ChooseRt.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CreateRt.route) {
            val authViewModel: AuthViewModel = viewModel {
                AuthViewModel(
                    container.getCurrentUserUseCase,
                    container.signInWithGoogleUseCase,
                    container.signOutUseCase,
                    container.createRtUseCase,
                    container.previewRtUseCase,
                    container.requestJoinRtUseCase,
                    container.cancelJoinRequestUseCase,
                    container.checkUsernameAvailableUseCase
                )
            }
            CreateRtScreen(
                viewModel = authViewModel,
                onBack = { rootNavController.popBackStack() },
                onRtCreated = {
                    rootNavController.navigate(Screen.Main.route) {
                        popUpTo(Screen.ChooseRt.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.HelpFaq.route) {
            HelpAndFaqScreen(
                onBack = { rootNavController.popBackStack() }
            )
        }

        composable(Screen.FinanceDashboard.route) {
            val vm: FinanceRecordViewModel = viewModel {
                FinanceRecordViewModel(
                    container.getCurrentUserUseCase,
                    container.getFinanceRecordsUseCase
                )
            }
            FinanceDashboardScreen(
                viewModel = vm,
                onBack = { rootNavController.popBackStack() }
            )
        }

        composable(Screen.ResidentDirectory.route) {
            val vm: ResidentDirectoryViewModel = viewModel {
                ResidentDirectoryViewModel(
                    container.getCurrentUserUseCase,
                    container.getWargaListUseCase
                )
            }
            ResidentDirectoryScreen(
                viewModel = vm,
                onBack = { rootNavController.popBackStack() }
            )
        }

        composable(Screen.CalendarRsvp.route) {
            val vm: CalendarRsvpViewModel = viewModel {
                CalendarRsvpViewModel(
                    container.getCurrentUserUseCase,
                    container.getRtFeedUseCase,
                    container.getAgendaRsvpsUseCase,
                    container.submitRsvpUseCase,
                    container.syncFeedUseCase
                )
            }
            CalendarRsvpScreen(
                viewModel = vm,
                onBack = { rootNavController.popBackStack() }
            )
        }

        composable(Screen.Forum.route) {
            val vm: ForumViewModel = viewModel {
                ForumViewModel(
                    container.getCurrentUserUseCase,
                    container.getRtFeedUseCase,
                    container.createPostUseCase,
                    container.syncFeedUseCase
                )
            }
            DiscussionForumScreen(
                viewModel = vm,
                onBack = { rootNavController.popBackStack() }
            )
        }

        composable(Screen.QrScanner.route) {
            val vm: AttendanceViewModel = viewModel {
                AttendanceViewModel(
                    container.getCurrentUserUseCase,
                    container.logAttendanceUseCase,
                    container.getAttendanceHistoryUseCase
                )
            }
            QrAttendanceScannerScreen(
                viewModel = vm,
                onBack = { rootNavController.popBackStack() }
            )
        }

        composable(Screen.Main.route) {
            MainTabsScaffold(
                container = container,
                onNavigateHelpFaq = {
                    rootNavController.navigate(Screen.HelpFaq.route)
                },
                onNavigateFinanceDashboard = {
                    rootNavController.navigate(Screen.FinanceDashboard.route)
                },
                onNavigateResidentDirectory = {
                    rootNavController.navigate(Screen.ResidentDirectory.route)
                },
                onNavigateCalendarRsvp = {
                    rootNavController.navigate(Screen.CalendarRsvp.route)
                },
                onNavigateForum = {
                    rootNavController.navigate(Screen.Forum.route)
                },
                onNavigateQrScanner = {
                    rootNavController.navigate(Screen.QrScanner.route)
                },
                onLoggedOut = {
                    rootNavController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun MainTabsScaffold(
    container: AppContainer,
    onNavigateHelpFaq: () -> Unit,
    onNavigateFinanceDashboard: () -> Unit,
    onNavigateResidentDirectory: () -> Unit,
    onNavigateCalendarRsvp: () -> Unit,
    onNavigateForum: () -> Unit,
    onNavigateQrScanner: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabs = remember {
        listOf(
            BottomNavTab.Home,
            BottomNavTab.Members,
            BottomNavTab.Finance,
            BottomNavTab.Settings
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            bottomNavController.navigate(tab.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.labelRes)
                            )
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavTab.Home.route,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable(BottomNavTab.Home.route) {
                val homeViewModel: HomeViewModel = viewModel {
                    HomeViewModel(
                        container.getCurrentUserUseCase,
                        container.getRtFeedUseCase,
                        container.syncFeedUseCase,
                        container.createPostUseCase,
                        container.pinPostUseCase,
                        container.unpinPostUseCase,
                        container.deletePostUseCase,
                        container.rtRepository,
                        container.getRtMembersUseCase,
                        container.syncMembersUseCase,
                        container.postImageProcessor
                    )
                }
                HomeScreen(viewModel = homeViewModel, imageLoader = container.imageLoader)
            }

            composable(BottomNavTab.Members.route) {
                val membersViewModel: MembersViewModel = viewModel {
                    MembersViewModel(
                        container.getCurrentUserUseCase,
                        container.getRtMembersUseCase,
                        container.syncMembersUseCase,
                        container.listPendingJoinRequestsUseCase,
                        container.approveJoinRequestUseCase,
                        container.rejectJoinRequestUseCase,
                        container.transferAdminUseCase,
                        container.removeMemberUseCase
                    )
                }
                MembersScreen(viewModel = membersViewModel)
            }

            composable(BottomNavTab.Finance.route) {
                val financeViewModel: FinanceViewModel = viewModel {
                    FinanceViewModel(
                        container.getCurrentUserUseCase,
                        container.getFinanceCategoriesUseCase,
                        container.getTransactionsUseCase,
                        container.syncFinancesUseCase,
                        container.createTransactionUseCase,
                        container.deleteTransactionUseCase,
                        container.publishFinanceReportUseCase,
                        container.publishMonthlyRecapUseCase,
                        container.createFinanceAgendaUseCase,
                        container.createFinanceCategoryUseCase,
                        container.updateFinanceCategoryUseCase,
                        container.financeRepository,
                        container.getRtMembersUseCase,
                        container.syncMembersUseCase,
                        container.assignBendaharaUseCase
                    )
                }
                FinanceScreen(viewModel = financeViewModel)
            }

            composable(BottomNavTab.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel {
                    SettingsViewModel(
                        container.getCurrentUserUseCase,
                        container.updateProfileUseCase,
                        container.signOutUseCase,
                        container.leaveRtUseCase,
                        container.setAutoApproveUseCase,
                        container.setInviteUsernameUseCase,
                        container.updateRtInfoUseCase,
                        container.rtRepository
                    )
                }
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onLoggedOut = onLoggedOut,
                    onNavigateHelpFaq = onNavigateHelpFaq,
                    onNavigateResidentDirectory = onNavigateResidentDirectory,
                    onNavigateFinanceDashboard = onNavigateFinanceDashboard,
                    onNavigateCalendarRsvp = onNavigateCalendarRsvp,
                    onNavigateForum = onNavigateForum,
                    onNavigateQrScanner = onNavigateQrScanner
                )
            }
        }
    }
}
