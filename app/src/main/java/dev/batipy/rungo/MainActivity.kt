package dev.batipy.rungo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.batipy.rungo.ui.home.HomeScreen
import dev.batipy.rungo.ui.login.LoginScreen
import dev.batipy.rungo.ui.login.LoginUiState
import dev.batipy.rungo.ui.login.LoginViewModel
import dev.batipy.rungo.ui.services.ServicesViewModel
import dev.batipy.rungo.ui.theme.RunGoTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as RunGoApplication

        setContent {
            RunGoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val context = LocalContext.current.applicationContext
                        var checkingSession by remember { mutableStateOf(true) }
                        val tokens by app.tokenStore.tokens.collectAsState()
                        val loggedIn = tokens != null

                        LaunchedEffect(Unit) {
                            app.authRepository.isLoggedIn()
                            checkingSession = false
                        }

                        if (!checkingSession) {
                            if (loggedIn) {
                                val servicesViewModel: ServicesViewModel = viewModel(
                                    factory = ServicesViewModel.Factory(
                                        app.catalogRepository,
                                        app.ordersRepository,
                                        context
                                    )
                                )
                                HomeScreen(
                                    servicesViewModel = servicesViewModel,
                                    catalogRepository = app.catalogRepository,
                                    ordersRepository = app.ordersRepository,
                                    profileRepository = app.profileRepository,
                                    locationProvider = app.locationProvider,
                                    onLogoutClick = {
                                        app.applicationScope.launch {
                                            app.authRepository.logout()
                                        }
                                    },
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
