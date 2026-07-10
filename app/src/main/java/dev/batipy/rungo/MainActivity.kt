package dev.batipy.rungo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.batipy.rungo.ui.home.HomeScreen
import dev.batipy.rungo.ui.login.LoginScreen
import dev.batipy.rungo.ui.login.LoginUiState
import dev.batipy.rungo.ui.login.LoginViewModel
import dev.batipy.rungo.ui.register.RegisterScreen
import dev.batipy.rungo.ui.register.RegisterViewModel
import dev.batipy.rungo.ui.services.ServicesViewModel
import dev.batipy.rungo.ui.theme.RunGoTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var pendingOrderId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingOrderId = extractOrderId(intent)

        val app = application as RunGoApplication

        setContent {
            RunGoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val context = LocalContext.current.applicationContext
                        var checkingSession by remember { mutableStateOf(true) }
                        var showRegister by remember { mutableStateOf(false) }
                        val tokens by app.tokenStore.tokens.collectAsState()
                        val loggedIn = tokens != null

                        val notificationPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { /* no-op either way — notifications just won't show if denied */ }

                        LaunchedEffect(Unit) {
                            app.authRepository.isLoggedIn()
                            checkingSession = false
                        }

                        LaunchedEffect(loggedIn) {
                            if (loggedIn) {
                                app.notificationRepository.registerCurrentDeviceToken()
                                app.orderFeedRepository.connect()
                            }
                        }

                        LaunchedEffect(Unit) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }

                        if (!checkingSession) {
                            if (loggedIn) {
                                val servicesViewModel: ServicesViewModel = viewModel(
                                    factory = ServicesViewModel.Factory(
                                        app.catalogRepository,
                                        app.ordersRepository,
                                        app.profileRepository,
                                        app.orderFeedRepository,
                                        context
                                    )
                                )
                                HomeScreen(
                                    servicesViewModel = servicesViewModel,
                                    catalogRepository = app.catalogRepository,
                                    ordersRepository = app.ordersRepository,
                                    profileRepository = app.profileRepository,
                                    locationProvider = app.locationProvider,
                                    cartRepository = app.cartRepository,
                                    chatRepository = app.chatRepository,
                                    orderFeedRepository = app.orderFeedRepository,
                                    initialOrderId = pendingOrderId,
                                    onInitialOrderConsumed = { pendingOrderId = null },
                                    onLogoutClick = {
                                        showRegister = false
                                        // All screen ViewModels (Services/Orders/Profile/Shop/Cart/...)
                                        // are scoped to this Activity's ViewModelStore, which otherwise
                                        // outlives login/logout — without clearing it here, logging in
                                        // as a different account would keep showing the previous
                                        // account's already-loaded data until each screen happened to
                                        // refetch on its own.
                                        viewModelStore.clear()
                                        app.cartRepository.clear()
                                        app.orderFeedRepository.disconnect()
                                        app.applicationScope.launch {
                                            app.notificationRepository.deleteCurrentDeviceToken()
                                            app.authRepository.logout()
                                        }
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            } else if (showRegister) {
                                val registerViewModel: RegisterViewModel = viewModel(
                                    factory = RegisterViewModel.Factory(
                                        app.authRepository,
                                        app.profileRepository,
                                        app.catalogRepository,
                                        context
                                    )
                                )
                                val uiState by registerViewModel.uiState.collectAsState()
                                val cities by registerViewModel.cities.collectAsState()

                                RegisterScreen(
                                    uiState = uiState,
                                    cities = cities,
                                    onRegisterClick = { username, password, password2, fullName, phone, cityId ->
                                        registerViewModel.register(username, password, password2, fullName, phone, cityId)
                                    },
                                    onLoginClick = { showRegister = false },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            } else {
                                val loginViewModel: LoginViewModel = viewModel(
                                    factory = LoginViewModel.Factory(app.authRepository, context)
                                )
                                val uiState by loginViewModel.uiState.collectAsState()

                                LoginScreen(
                                    uiState = uiState,
                                    onLoginClick = { username, password ->
                                        loginViewModel.login(username, password)
                                    },
                                    onRegisterClick = { showRegister = true },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOrderId = extractOrderId(intent)
    }

    private fun extractOrderId(intent: Intent): Int? =
        intent.getIntExtra(EXTRA_ORDER_ID, -1).takeIf { it != -1 }

    companion object {
        const val EXTRA_ORDER_ID = "extra_order_id"
    }
}
