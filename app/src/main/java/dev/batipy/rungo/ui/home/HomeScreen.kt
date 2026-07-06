package dev.batipy.rungo.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.batipy.rungo.R
import dev.batipy.rungo.data.cart.CartRepository
import dev.batipy.rungo.data.catalog.CatalogRepository
import dev.batipy.rungo.data.location.LocationProvider
import dev.batipy.rungo.data.network.dto.ServiceDto
import dev.batipy.rungo.data.orders.OrdersRepository
import dev.batipy.rungo.data.profile.ProfileRepository
import dev.batipy.rungo.ui.cart.CartScreen
import dev.batipy.rungo.ui.cart.CartViewModel
import dev.batipy.rungo.ui.orders.OrderDetailScreen
import dev.batipy.rungo.ui.orders.OrderDetailViewModel
import dev.batipy.rungo.ui.orders.OrdersScreen
import dev.batipy.rungo.ui.orders.OrdersViewModel
import dev.batipy.rungo.ui.profile.ProfileScreen
import dev.batipy.rungo.ui.profile.ProfileViewModel
import dev.batipy.rungo.ui.services.CreateOrderViewModel
import dev.batipy.rungo.ui.services.ServiceOrderScreen
import dev.batipy.rungo.ui.services.ServicesScreen
import dev.batipy.rungo.ui.services.ServicesViewModel
import dev.batipy.rungo.ui.shop.MerchantDetailScreen
import dev.batipy.rungo.ui.shop.MerchantDetailViewModel
import dev.batipy.rungo.ui.shop.ShopScreen
import dev.batipy.rungo.ui.shop.ShopViewModel

private data class HomeTab(val label: String, val icon: ImageVector)

@Composable
private fun homeTabs(): List<HomeTab> = listOf(
    HomeTab(stringResource(R.string.nav_services), Icons.Filled.Menu),
    HomeTab(stringResource(R.string.nav_shop), Icons.Filled.ShoppingCart),
    HomeTab(stringResource(R.string.nav_orders), Icons.AutoMirrored.Filled.Assignment),
    HomeTab(stringResource(R.string.nav_cart), Icons.Filled.ShoppingBag),
    HomeTab(stringResource(R.string.nav_profile), Icons.Filled.Person)
)

@Composable
fun HomeScreen(
    servicesViewModel: ServicesViewModel,
    catalogRepository: CatalogRepository,
    ordersRepository: OrdersRepository,
    profileRepository: ProfileRepository,
    locationProvider: LocationProvider,
    cartRepository: CartRepository,
    initialOrderId: Int? = null,
    onInitialOrderConsumed: () -> Unit = {},
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    // Set from either the active-order banner on Услуги, a tap on an order in
    // Заказы, or a tapped push notification (initialOrderId) — overrides
    // whatever tab content would otherwise show.
    var selectedOrderId by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(initialOrderId) {
        if (initialOrderId != null) {
            selectedOrderId = initialOrderId
            onInitialOrderConsumed()
        }
    }

    // Hoisted (rather than created inside the `when` branch below) so the bottom
    // bar's onClick can trigger a refresh even when tapping a tab you're already on.
    val ordersViewModel: OrdersViewModel = viewModel(
        factory = OrdersViewModel.Factory(ordersRepository, context)
    )
    val cartItems by cartRepository.items.collectAsState()
    val cartCount = cartItems.sumOf { it.quantity }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                homeTabs().forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index && selectedOrderId == null,
                        onClick = {
                            // Only refresh when actually navigating to this tab (a
                            // different tab, or coming back from order detail) —
                            // re-tapping the tab you're already on shouldn't re-hit
                            // the network every time.
                            val isNavigatingHere = selectedTab != index || selectedOrderId != null
                            selectedTab = index
                            selectedOrderId = null
                            if (isNavigatingHere) {
                                when (index) {
                                    0 -> servicesViewModel.refresh()
                                    2 -> ordersViewModel.refresh()
                                }
                            }
                        },
                        icon = {
                            if (index == 3 && cartCount > 0) {
                                BadgedBox(badge = { Badge { Text("$cartCount") } }) {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val orderId = selectedOrderId
        if (orderId != null) {
            val orderDetailViewModel: OrderDetailViewModel = viewModel(
                key = "order-detail-$orderId",
                factory = OrderDetailViewModel.Factory(orderId, ordersRepository, profileRepository, catalogRepository, context)
            )
            val orderDetailState by orderDetailViewModel.uiState.collectAsState()
            val orderDetailMessage by orderDetailViewModel.message.collectAsState()
            val orderDetailRefreshing by orderDetailViewModel.isRefreshing.collectAsState()
            OrderDetailScreen(
                uiState = orderDetailState,
                message = orderDetailMessage,
                isRefreshing = orderDetailRefreshing,
                onRefresh = orderDetailViewModel::refresh,
                onConsumeMessage = orderDetailViewModel::consumeMessage,
                onBack = { selectedOrderId = null },
                onCancelOrder = orderDetailViewModel::cancelOrder,
                onRequestConfirmDelivery = orderDetailViewModel::requestConfirmDelivery,
                onCancelConfirmDelivery = orderDetailViewModel::cancelConfirmDelivery,
                onConfirmDelivery = orderDetailViewModel::confirmDelivery,
                onSelectRating = orderDetailViewModel::selectReviewRating,
                onReviewTextChange = orderDetailViewModel::updateReviewText,
                onSubmitReview = orderDetailViewModel::submitReview,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
        when (selectedTab) {
            0 -> {
                var selectedService by remember { mutableStateOf<ServiceDto?>(null) }
                // Bumped every time a service is opened so re-opening the same
                // service after an order was created gets a fresh ViewModel
                // instead of one whose orderCreated flag is still true.
                var orderFormToken by remember { mutableIntStateOf(0) }
                var selectedMerchantId by remember { mutableStateOf<Int?>(null) }
                val service = selectedService
                val merchantId = selectedMerchantId

                if (merchantId != null) {
                    val merchantDetailViewModel: MerchantDetailViewModel = viewModel(
                        key = "merchant-$merchantId",
                        factory = MerchantDetailViewModel.Factory(merchantId, catalogRepository, cartRepository, context)
                    )
                    val merchantUiState by merchantDetailViewModel.uiState.collectAsState()
                    val merchantCartItems by merchantDetailViewModel.cartItems.collectAsState()
                    MerchantDetailScreen(
                        uiState = merchantUiState,
                        cartItems = merchantCartItems,
                        onBack = { selectedMerchantId = null },
                        onAddToCart = merchantDetailViewModel::addToCart,
                        onUpdateQuantity = merchantDetailViewModel::updateQuantity,
                        onGoToCart = {
                            selectedMerchantId = null
                            selectedTab = 3
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else if (service == null) {
                    val uiState by servicesViewModel.uiState.collectAsState()
                    val isRefreshing by servicesViewModel.isRefreshing.collectAsState()
                    ServicesScreen(
                        uiState = uiState,
                        isRefreshing = isRefreshing,
                        onRefresh = servicesViewModel::refresh,
                        onServiceClick = {
                            selectedService = it
                            orderFormToken++
                        },
                        onActiveOrderClick = { order -> selectedOrderId = order.id },
                        onMerchantClick = { merchant -> selectedMerchantId = merchant.id },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    val createOrderViewModel: CreateOrderViewModel = viewModel(
                        key = "${service.id}-$orderFormToken",
                        factory = CreateOrderViewModel.Factory(
                            catalogRepository = catalogRepository,
                            profileRepository = profileRepository,
                            ordersRepository = ordersRepository,
                            context = context
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
                        onPickupLocationSelect = createOrderViewModel::selectPickupLocation,
                        onPickupManualEntrySelect = createOrderViewModel::selectPickupManualEntry,
                        onPickupManualAddressChange = createOrderViewModel::updatePickupManualAddress,
                        onLocationSelect = createOrderViewModel::selectLocation,
                        onManualEntrySelect = createOrderViewModel::selectManualEntry,
                        onManualAddressChange = createOrderViewModel::updateManualAddress,
                        onCommentChange = createOrderViewModel::updateComment,
                        onCurrencySelect = createOrderViewModel::selectCurrency,
                        onSubmit = { createOrderViewModel.submit(service.id, service.kind) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            1 -> {
                var selectedMerchantId by remember { mutableStateOf<Int?>(null) }
                val merchantId = selectedMerchantId

                if (merchantId == null) {
                    val shopViewModel: ShopViewModel = viewModel(
                        factory = ShopViewModel.Factory(catalogRepository, profileRepository, context)
                    )
                    val uiState by shopViewModel.uiState.collectAsState()
                    val isRefreshing by shopViewModel.isRefreshing.collectAsState()
                    ShopScreen(
                        uiState = uiState,
                        isRefreshing = isRefreshing,
                        onRefresh = shopViewModel::refresh,
                        onMerchantClick = { merchant -> selectedMerchantId = merchant.id },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    val merchantDetailViewModel: MerchantDetailViewModel = viewModel(
                        key = "merchant-$merchantId",
                        factory = MerchantDetailViewModel.Factory(merchantId, catalogRepository, cartRepository, context)
                    )
                    val uiState by merchantDetailViewModel.uiState.collectAsState()
                    val merchantCartItems by merchantDetailViewModel.cartItems.collectAsState()
                    MerchantDetailScreen(
                        uiState = uiState,
                        cartItems = merchantCartItems,
                        onBack = { selectedMerchantId = null },
                        onAddToCart = merchantDetailViewModel::addToCart,
                        onUpdateQuantity = merchantDetailViewModel::updateQuantity,
                        onGoToCart = {
                            selectedMerchantId = null
                            selectedTab = 3
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            2 -> {
                val uiState by ordersViewModel.uiState.collectAsState()
                val isRefreshing by ordersViewModel.isRefreshing.collectAsState()
                OrdersScreen(
                    uiState = uiState,
                    isRefreshing = isRefreshing,
                    onRefresh = ordersViewModel::refresh,
                    onOrderClick = { order -> selectedOrderId = order.id },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            3 -> {
                val cartViewModel: CartViewModel = viewModel(
                    factory = CartViewModel.Factory(cartRepository, catalogRepository, ordersRepository, profileRepository, context)
                )
                val uiState by cartViewModel.uiState.collectAsState()
                val orderCreated by cartViewModel.orderCreated.collectAsState()

                LaunchedEffect(orderCreated) {
                    val createdId = orderCreated
                    if (createdId != null) {
                        selectedOrderId = createdId
                        cartViewModel.consumeOrderCreated()
                    }
                }

                CartScreen(
                    uiState = uiState,
                    cartItems = cartItems,
                    onGoToShopClick = { selectedTab = 1 },
                    onUpdateQuantity = cartViewModel::updateItemQuantity,
                    onRemoveItem = cartViewModel::removeItem,
                    onLocationSelect = cartViewModel::selectLocation,
                    onManualEntrySelect = cartViewModel::selectManualEntry,
                    onManualAddressChange = cartViewModel::updateManualAddress,
                    onCommentChange = cartViewModel::updateComment,
                    onCurrencySelect = cartViewModel::selectCurrency,
                    onSubmit = cartViewModel::submit,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            4 -> {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.Factory(profileRepository, catalogRepository, locationProvider, context)
                )
                val uiState by profileViewModel.uiState.collectAsState()
                val message by profileViewModel.message.collectAsState()
                val isRefreshing by profileViewModel.isRefreshing.collectAsState()
                val isAddingLocation by profileViewModel.addingLocation.collectAsState()
                val isUpdatingProfile by profileViewModel.updatingProfile.collectAsState()
                ProfileScreen(
                    uiState = uiState,
                    message = message,
                    isRefreshing = isRefreshing,
                    onRefresh = profileViewModel::refresh,
                    onConsumeMessage = profileViewModel::consumeMessage,
                    onDeleteLocation = profileViewModel::deleteLocation,
                    onRequestLocation = profileViewModel::addCurrentLocation,
                    isAddingLocation = isAddingLocation,
                    onLocationPermissionDenied = profileViewModel::locationPermissionDenied,
                    onLanguageSelect = profileViewModel::setLanguage,
                    onSaveProfile = profileViewModel::updateProfile,
                    isUpdatingProfile = isUpdatingProfile,
                    onSendSupportMessage = profileViewModel::sendSupportMessage,
                    onLogoutClick = onLogoutClick,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        }
    }
}
