package dev.batipy.rungo.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.batipy.rungo.data.catalog.CatalogRepository
import dev.batipy.rungo.data.network.dto.ServiceDto
import dev.batipy.rungo.data.orders.OrdersRepository
import dev.batipy.rungo.data.profile.ProfileRepository
import dev.batipy.rungo.ui.cart.CartScreen
import dev.batipy.rungo.ui.orders.OrdersScreen
import dev.batipy.rungo.ui.orders.OrdersViewModel
import dev.batipy.rungo.ui.profile.ProfileScreen
import dev.batipy.rungo.ui.profile.ProfileViewModel
import dev.batipy.rungo.ui.services.CreateOrderViewModel
import dev.batipy.rungo.ui.services.ServiceOrderScreen
import dev.batipy.rungo.ui.services.ServicesScreen
import dev.batipy.rungo.ui.services.ServicesViewModel
import dev.batipy.rungo.ui.shop.ShopScreen
import dev.batipy.rungo.ui.shop.ShopViewModel

private data class HomeTab(val label: String, val icon: ImageVector)

private val homeTabs = listOf(
    HomeTab("Услуги", Icons.Filled.Menu),
    HomeTab("Магазин", Icons.Filled.ShoppingCart),
    HomeTab("Заказы", Icons.AutoMirrored.Filled.Assignment),
    HomeTab("Корзина", Icons.Filled.ShoppingBag),
    HomeTab("Профиль", Icons.Filled.Person)
)

@Composable
fun HomeScreen(
    servicesViewModel: ServicesViewModel,
    catalogRepository: CatalogRepository,
    ordersRepository: OrdersRepository,
    profileRepository: ProfileRepository,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                homeTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> {
                var selectedService by remember { mutableStateOf<ServiceDto?>(null) }
                val service = selectedService

                if (service == null) {
                    val uiState by servicesViewModel.uiState.collectAsState()
                    val isRefreshing by servicesViewModel.isRefreshing.collectAsState()
                    ServicesScreen(
                        uiState = uiState,
                        isRefreshing = isRefreshing,
                        onRefresh = servicesViewModel::refresh,
                        onServiceClick = { selectedService = it },
                        onActiveOrderClick = { selectedTab = 2 },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    val createOrderViewModel: CreateOrderViewModel = viewModel(
                        key = service.id.toString(),
                        factory = CreateOrderViewModel.Factory(
                            catalogRepository = catalogRepository,
                            profileRepository = profileRepository,
                            ordersRepository = ordersRepository
                        )
                    )
                    val orderUiState by createOrderViewModel.uiState.collectAsState()
                    val orderCreated by createOrderViewModel.orderCreated.collectAsState()

                    LaunchedEffect(orderCreated) {
                        if (orderCreated) {
                            selectedService = null
                        }
                    }

                    ServiceOrderScreen(
                        service = service,
                        uiState = orderUiState,
                        onBack = { selectedService = null },
                        onCitySelect = createOrderViewModel::selectCity,
                        onLocationSelect = createOrderViewModel::selectLocation,
                        onManualEntrySelect = createOrderViewModel::selectManualEntry,
                        onManualAddressChange = createOrderViewModel::updateManualAddress,
                        onCommentChange = createOrderViewModel::updateComment,
                        onCurrencySelect = createOrderViewModel::selectCurrency,
                        onSubmit = { createOrderViewModel.submit(service.id) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            1 -> {
                val shopViewModel: ShopViewModel = viewModel(
                    factory = ShopViewModel.Factory(catalogRepository)
                )
                val uiState by shopViewModel.uiState.collectAsState()
                val isRefreshing by shopViewModel.isRefreshing.collectAsState()
                ShopScreen(
                    uiState = uiState,
                    isRefreshing = isRefreshing,
                    onRefresh = shopViewModel::refresh,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            2 -> {
                val ordersViewModel: OrdersViewModel = viewModel(
                    factory = OrdersViewModel.Factory(ordersRepository)
                )
                val uiState by ordersViewModel.uiState.collectAsState()
                val isRefreshing by ordersViewModel.isRefreshing.collectAsState()
                OrdersScreen(
                    uiState = uiState,
                    isRefreshing = isRefreshing,
                    onRefresh = ordersViewModel::refresh,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            3 -> CartScreen(
                onGoToShopClick = { selectedTab = 1 },
                modifier = Modifier.padding(innerPadding)
            )

            4 -> {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.Factory(profileRepository)
                )
                val uiState by profileViewModel.uiState.collectAsState()
                val message by profileViewModel.message.collectAsState()
                val isRefreshing by profileViewModel.isRefreshing.collectAsState()
                ProfileScreen(
                    uiState = uiState,
                    message = message,
                    isRefreshing = isRefreshing,
                    onRefresh = profileViewModel::refresh,
                    onConsumeMessage = profileViewModel::consumeMessage,
                    onDeleteLocation = profileViewModel::deleteLocation,
                    onRequestLocation = profileViewModel::requestLocationViaBot,
                    onLanguageSelect = profileViewModel::setLanguage,
                    onSendSupportMessage = profileViewModel::sendSupportMessage,
                    onLogoutClick = onLogoutClick,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
