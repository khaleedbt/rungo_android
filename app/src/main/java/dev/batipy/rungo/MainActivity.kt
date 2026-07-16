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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.batipy.rungo.ui.home.HomeScreen
import dev.batipy.rungo.ui.login.LoginScreen
import dev.batipy.rungo.ui.login.LoginUiState
import dev.batipy.rungo.ui.login.LoginViewModel
import dev.batipy.rungo.ui.register.RegisterScreen
import dev.batipy.rungo.ui.register.RegisterViewModel
import dev.batipy.rungo.ui.services.ServicesViewModel
import dev.batipy.rungo.ui.theme.RunGoLightBackground
import dev.batipy.rungo.ui.theme.RunGoTheme
import kotlinx.coroutines.launch

// Lets a screen deep in the tree (e.g. the courier light-theme trial) ask for
// light system bars (dark icons, light scrim behind the status/navigation bar
// strip) without the whole app switching color scheme — RunGoTheme itself
// stays a single fixed dark MaterialTheme. Defaults to true because Login is
// the first screen shown and is light; HomeScreen overrides this per-role
// right after login via its own DisposableEffect(isLight).
val LocalSetLightSystemBars = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

class MainActivity : AppCompatActivity() {

    private var pendingOrderId by mutableStateOf<Int?>(null)
    private var pendingChatOrderId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingOrderId = extractOrderId(intent)
        pendingChatOrderId = extractChatOrderId(intent)

        val app = application as RunGoApplication

        setContent {
            RunGoTheme {
                var lightSystemBars by remember { mutableStateOf(true) }
                val view = LocalView.current
                // Reading lightSystemBars right here (composition phase, not
                // inside the effect below) is what makes this scope — and so
                // SideEffect, which reruns whenever its containing scope does
                // — actually re-fire when it changes. A read that only
                // happens inside SideEffect's own lambda body doesn't count:
                // that runs in the effect phase, which Compose doesn't track
                // for recomposition, so the callback would silently go stale.
                val currentLightSystemBars = lightSystemBars
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = currentLightSystemBars
                    controller.isAppearanceLightNavigationBars = currentLightSystemBars
                }

                CompositionLocalProvider(LocalSetLightSystemBars provides { lightSystemBars = it }) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (lightSystemBars) RunGoLightBackground else MaterialTheme.colorScheme.surface
                    ) {
                        val context = LocalContext.current.applicationContext
                        var checkingSession by rememberSaveable { mutableStateOf(true) }
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

                        // Android kills background sockets fairly aggressively, so a
                        // status change that happens while the app is backgrounded can
                        // be missed entirely — force a reconnect + refresh whenever the
                        // app comes back to the foreground, on top of the live feed.
                        val lifecycleOwner = LocalLifecycleOwner.current
                        DisposableEffect(lifecycleOwner) {
                            val observer = LifecycleEventObserver { _, event ->
                                if (event == Lifecycle.Event.ON_RESUME) {
                                    app.orderFeedRepository.onAppResumed()
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                                    orderLocationRepository = app.orderLocationRepository,
                                    authRepository = app.authRepository,
                                    initialOrderId = pendingOrderId,
                                    onInitialOrderConsumed = { pendingOrderId = null },
                                    initialChatOrderId = pendingChatOrderId,
                                    onInitialChatOrderConsumed = { pendingChatOrderId = null },
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
                                    // HomeScreen has its own nested Scaffold, which by
                                    // default re-applies systemBars/displayCutout insets
                                    // on top of what this outer Scaffold already
                                    // consumed — consumeWindowInsets tells it those are
                                    // already accounted for so it doesn't double them.
                                    modifier = Modifier
                                        .padding(innerPadding)
                                        .consumeWindowInsets(innerPadding)
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOrderId = extractOrderId(intent)
        pendingChatOrderId = extractChatOrderId(intent)
    }

    private fun extractOrderId(intent: Intent): Int? =
        intent.getIntExtra(EXTRA_ORDER_ID, -1).takeIf { it != -1 }

    private fun extractChatOrderId(intent: Intent): Int? =
        intent.getIntExtra(EXTRA_CHAT_ORDER_ID, -1).takeIf { it != -1 }

    companion object {
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_CHAT_ORDER_ID = "extra_chat_order_id"
    }
}
