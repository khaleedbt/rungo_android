package dev.batipy.rungo.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.batipy.rungo.LocalSetLightSystemBars
import dev.batipy.rungo.R
import dev.batipy.rungo.data.auth.AuthRepository
import dev.batipy.rungo.data.cart.CartRepository
import dev.batipy.rungo.data.shop.ShopDisplayPrefs
import dev.batipy.rungo.data.catalog.CatalogRepository
import dev.batipy.rungo.data.chat.ChatRepository
import dev.batipy.rungo.data.location.CourierLocationService
import dev.batipy.rungo.data.location.LocationProvider
import dev.batipy.rungo.data.location.OrderLocationRepository
import dev.batipy.rungo.data.network.dto.ServiceDto
import dev.batipy.rungo.data.orders.OrderFeedRepository
import dev.batipy.rungo.data.orders.OrdersRepository
import dev.batipy.rungo.data.profile.ProfileRepository
import dev.batipy.rungo.ui.cart.CartScreen
import dev.batipy.rungo.ui.cart.CartUiState
import dev.batipy.rungo.ui.cart.CartViewModel
import dev.batipy.rungo.ui.chat.ChatScreen
import dev.batipy.rungo.ui.chat.ChatViewModel
import dev.batipy.rungo.ui.courier.CourierOrderDetailScreen
import dev.batipy.rungo.ui.courier.CourierOrderDetailViewModel
import dev.batipy.rungo.ui.courier.CourierOrdersScreen
import dev.batipy.rungo.ui.courier.CourierOrdersViewModel
import dev.batipy.rungo.ui.orders.OrderDetailScreen
import dev.batipy.rungo.ui.orders.OrderDetailViewModel
import dev.batipy.rungo.ui.orders.OrdersScreen
import dev.batipy.rungo.ui.orders.OrdersViewModel
import dev.batipy.rungo.ui.partner.PartnerOrdersScreen
import dev.batipy.rungo.ui.partner.PartnerOrdersViewModel
import dev.batipy.rungo.ui.profile.ProfileScreen
import dev.batipy.rungo.ui.profile.ProfileViewModel
import dev.batipy.rungo.ui.services.CreateOrderUiState
import dev.batipy.rungo.ui.services.CreateOrderViewModel
import dev.batipy.rungo.ui.services.ServiceOrderScreen
import dev.batipy.rungo.ui.services.ServicesScreen
import dev.batipy.rungo.ui.services.ServicesViewModel
import dev.batipy.rungo.ui.shop.MerchantDetailScreen
import dev.batipy.rungo.ui.shop.MerchantDetailViewModel
import dev.batipy.rungo.ui.tracking.OrderTrackingScreen
import dev.batipy.rungo.ui.tracking.OrderTrackingViewModel
import dev.batipy.rungo.ui.shop.ShopScreen
import dev.batipy.rungo.ui.shop.ShopViewModel
import dev.batipy.rungo.ui.theme.RunGoBrandOrange
import dev.batipy.rungo.ui.theme.RunGoLightAccentText
import dev.batipy.rungo.ui.theme.RunGoLightField
import dev.batipy.rungo.ui.theme.RunGoLightTextSecondary
import dev.batipy.rungo.ui.theme.RunGoOnBrandOrange
import dev.batipy.rungo.ui.theme.RunGoTextSecondary

private data class HomeTab(val label: String, val icon: ImageVector)

@Composable
private fun homeTabs(): List<HomeTab> = listOf(
    HomeTab(stringResource(R.string.nav_services), Icons.Filled.Menu),
    HomeTab(stringResource(R.string.nav_shop), Icons.Filled.ShoppingCart),
    HomeTab(stringResource(R.string.nav_orders), Icons.AutoMirrored.Filled.Assignment),
    HomeTab(stringResource(R.string.nav_cart), Icons.Filled.ShoppingBag),
    HomeTab(stringResource(R.string.nav_profile), Icons.Filled.Person)
)

// Courier accounts don't shop/order — just a queue of deliveries to work and a profile.
@Composable
private fun courierHomeTabs(): List<HomeTab> = listOf(
    HomeTab(stringResource(R.string.nav_orders), Icons.AutoMirrored.Filled.Assignment),
    HomeTab(stringResource(R.string.nav_profile), Icons.Filled.Person)
)

// Partner (merchant) accounts just track orders containing their products and a profile.
@Composable
private fun partnerHomeTabs(): List<HomeTab> = listOf(
    HomeTab(stringResource(R.string.partner_orders_title), Icons.Filled.Store),
    HomeTab(stringResource(R.string.nav_profile), Icons.Filled.Person)
)

// Snapshot of "what's on screen" for the outer AnimatedContent below — the id
// rides along on the target/initial state itself rather than being re-read
// from live (chatOrderId/trackingOrderId/selectedOrderId) state inside the
// content lambda. Reading the live state there would go null the instant the
// user navigates back (before the exit animation finishes), leaving the
// outgoing screen blank mid-transition instead of showing what it had.
private sealed class HomeContent {
    data class Chat(val orderId: Int, val currentUserId: Int) : HomeContent()
    data class Tracking(val orderId: Int) : HomeContent()
    data class CourierDetail(val orderId: Int) : HomeContent()
    data class ClientDetail(val orderId: Int) : HomeContent()
    data object Tabs : HomeContent()
}

// Same snapshot-the-target-state approach as HomeContent above, for the
// Услуги tab's own internal drill-down (catalog → merchant, or catalog →
// order form) — this one nests inside HomeContent.Tabs.
private sealed class ServicesTabContent {
    data class Merchant(val merchantId: Int) : ServicesTabContent()
    data class Order(val service: ServiceDto, val formToken: Int) : ServicesTabContent()
    data object Catalog : ServicesTabContent()
}

// Same idea for the Магазин tab's merchant drill-down.
private sealed class ShopTabContent {
    data class Merchant(val merchantId: Int) : ShopTabContent()
    data object Catalog : ShopTabContent()
}

@Composable
fun HomeScreen(
    servicesViewModel: ServicesViewModel,
    catalogRepository: CatalogRepository,
    ordersRepository: OrdersRepository,
    profileRepository: ProfileRepository,
    locationProvider: LocationProvider,
    cartRepository: CartRepository,
    chatRepository: ChatRepository,
    orderFeedRepository: OrderFeedRepository,
    orderLocationRepository: OrderLocationRepository,
    authRepository: AuthRepository,
    initialOrderId: Int? = null,
    onInitialOrderConsumed: () -> Unit = {},
    initialChatOrderId: Int? = null,
    onInitialChatOrderConsumed: () -> Unit = {},
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

    // Determines which screen set to show — fetched once on entry rather than
    // stored globally, matching how every other screen independently pulls
    // what it needs from ProfileRepository. currentUserId rides along on the
    // same call — needed to tell "my" chat bubbles apart from the other side's.
    var role by rememberSaveable { mutableStateOf<String?>(null) }
    var currentUserId by rememberSaveable { mutableStateOf<Int?>(null) }
    var roleLoadFailed by rememberSaveable { mutableStateOf(false) }
    var roleLoadAttempt by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(roleLoadAttempt) {
        val me = profileRepository.getMe().getOrNull()
        if (me != null) {
            role = me.role
            currentUserId = me.id
        } else {
            roleLoadFailed = true
        }
    }

    // CourierLocationService only gets (re)started from CourierOrderDetailViewModel,
    // i.e. while the courier has that specific order's screen open — if the app
    // process was killed mid-delivery (swiped away, phone restarted), the
    // foreground service dies with it and nothing else restarts it. Covering
    // that gap here: as soon as we know this is a courier, check for an order
    // already in_progress/in_delivery and resume tracking without requiring
    // them to open it again.
    LaunchedEffect(role) {
        if (role == "courier") {
            val activeOrder = ordersRepository.getCourierOrders().getOrNull()
                ?.firstOrNull { it.status == "in_progress" || it.status == "in_delivery" }
            if (activeOrder != null) {
                CourierLocationService.start(context, activeOrder.id)
            }
        }
    }

    // Set when either the client or the courier taps "Написать" on an order's
    // detail screen, or a tapped chat push notification (initialChatOrderId)
    // — overrides everything else, same as selectedOrderId.
    var chatOrderId by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(initialChatOrderId) {
        if (initialChatOrderId != null) {
            chatOrderId = initialChatOrderId
            onInitialChatOrderConsumed()
        }
    }

    // Set when the client taps "Отследить курьера на карте" on an order's
    // detail screen — same full-screen-overlay treatment as chatOrderId.
    var trackingOrderId by rememberSaveable { mutableStateOf<Int?>(null) }

    // Hoisted up here (rather than local state inside ShopScreen) so the
    // grid/list choice survives navigating into a merchant and back, and
    // switching tabs and back — it used to live inside ShopScreen itself and
    // reset on every such round trip since that composable gets torn down.
    // Backed by SharedPreferences (not just rememberSaveable) so it also
    // survives fully closing and reopening the app, not just backgrounding.
    val shopDisplayPrefs = remember { ShopDisplayPrefs(context) }
    var isShopGridView by rememberSaveable { mutableStateOf(shopDisplayPrefs.isGridView) }

    if (role == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (roleLoadFailed) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.home_role_load_error), color = RunGoTextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        roleLoadFailed = false
                        roleLoadAttempt++
                    }) {
                        Text(stringResource(R.string.common_retry))
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }
    val isCourier = role == "courier"
    val isPartner = role == "partner"
    // Light-theme rollout: every logged-in role (client, courier, partner) is
    // light now. Centralized here rather than in each leaf screen — those
    // still take their own `light` param for content colors, but the system
    // status/nav bar appearance only needs setting once for the whole
    // logged-in session, not on every screen transition (which was causing a
    // brief dark flash before).
    val isLight = true
    val setLightSystemBars = LocalSetLightSystemBars.current
    DisposableEffect(isLight) {
        setLightSystemBars(isLight)
        onDispose { }
    }

    // Hoisted (rather than created inside the `when` branch below) so the bottom
    // bar's onClick can trigger a refresh even when tapping a tab you're already on.
    val ordersViewModel: OrdersViewModel = viewModel(
        factory = OrdersViewModel.Factory(ordersRepository, orderFeedRepository, context)
    )
    val cartItems by cartRepository.items.collectAsState()
    val cartCount = cartItems.sumOf { it.quantity }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            // Scaffold always reserves bottomBar's height in innerPadding even
            // when this lambda renders nothing, so it has to be skipped
            // entirely (not just left showing) while a full-screen overlay
            // (chat, order/merchant detail) is up — otherwise that reserved
            // space shows up as unexplained empty space at the bottom of the
            // overlay, e.g. between the chat input and the keyboard.
            if (chatOrderId == null && selectedOrderId == null && trackingOrderId == null) {
                NavigationBar(
                    // Light-theme rollout — every role's screens are light
                    // now, so the nav bar follows; a shadow (rather than
                    // relying on tonalElevation, which needs the theme's own
                    // surface color to have any visible effect) is what
                    // actually separates a white bar from a white/light body.
                    modifier = if (isLight) Modifier.shadow(elevation = 12.dp) else Modifier,
                    containerColor = if (isLight) RunGoLightField else NavigationBarDefaults.containerColor
                ) {
                    (if (isCourier) courierHomeTabs() else if (isPartner) partnerHomeTabs() else homeTabs()).forEachIndexed { index, tab ->
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
                            if (isNavigatingHere && !isCourier && !isPartner) {
                                when (index) {
                                    0 -> servicesViewModel.refresh()
                                    2 -> ordersViewModel.refresh()
                                }
                            }
                        },
                        icon = {
                            if (!isCourier && index == 3 && cartCount > 0) {
                                BadgedBox(badge = { Badge { Text("$cartCount") } }) {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label) },
                        colors = if (isLight) {
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = RunGoOnBrandOrange,
                                selectedTextColor = RunGoLightAccentText,
                                indicatorColor = RunGoBrandOrange,
                                unselectedIconColor = RunGoLightTextSecondary,
                                unselectedTextColor = RunGoLightTextSecondary
                            )
                        } else {
                            NavigationBarItemDefaults.colors()
                        }
                    )
                    }
                }
            }
        }
    ) { innerPadding ->
        val homeContent: HomeContent = when {
            chatOrderId != null && currentUserId != null -> HomeContent.Chat(chatOrderId!!, currentUserId!!)
            trackingOrderId != null -> HomeContent.Tracking(trackingOrderId!!)
            selectedOrderId != null && isCourier -> HomeContent.CourierDetail(selectedOrderId!!)
            selectedOrderId != null -> HomeContent.ClientDetail(selectedOrderId!!)
            else -> HomeContent.Tabs
        }

        if (chatOrderId != null && currentUserId != null) {
            BackHandler { chatOrderId = null }
        } else if (trackingOrderId != null) {
            BackHandler { trackingOrderId = null }
        } else if (selectedOrderId != null) {
            BackHandler { selectedOrderId = null }
        }

        AnimatedContent(
            targetState = homeContent,
            transitionSpec = {
                // Drilling into a detail/chat/tracking screen slides in from
                // the right over the list beneath it; backing out of one
                // slides back out to the right — a directional "push", not a
                // flat crossfade, so forward/back reads as forward/back.
                val enteringDetail = targetState !is HomeContent.Tabs && initialState is HomeContent.Tabs
                val leavingDetail = targetState is HomeContent.Tabs && initialState !is HomeContent.Tabs
                when {
                    enteringDetail -> (slideInHorizontally(animationSpec = tween(260), initialOffsetX = { it }) + fadeIn(animationSpec = tween(220))) togetherWith
                        fadeOut(animationSpec = tween(140))
                    leavingDetail -> fadeIn(animationSpec = tween(180)) togetherWith
                        (slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { it }) + fadeOut(animationSpec = tween(180)))
                    else -> fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                }
            },
            label = "homeContent"
        ) { content ->
        when (content) {
            is HomeContent.Chat -> {
                val chatId = content.orderId
                val chatViewModel: ChatViewModel = viewModel(
                    key = "chat-$chatId",
                    factory = ChatViewModel.Factory(chatId, content.currentUserId, chatRepository, authRepository, context)
                )
                val chatState by chatViewModel.uiState.collectAsState()
                val chatMessage by chatViewModel.message.collectAsState()

                // No forced reconnect-on-reentry here (unlike the order-detail
                // ViewModels below) — the ViewModel's own init{} already starts
                // connecting once, and the socket stays live in the background
                // while this screen isn't shown, so re-opening the same chat
                // just shows whatever's current. Calling connect() again here
                // raced with that initial connect and closed the fresh socket
                // out from under itself, which is what caused the "не удалось
                // подключиться" flash right after tapping "Написать".

                ChatScreen(
                    orderId = chatId,
                    uiState = chatState,
                    currentUserId = chatViewModel.currentUserId,
                    message = chatMessage,
                    onConsumeMessage = chatViewModel::consumeMessage,
                    onBack = { chatOrderId = null },
                    onSend = chatViewModel::sendMessage,
                    onRetry = chatViewModel::connect,
                    light = isLight,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is HomeContent.Tracking -> {
                val trackId = content.orderId
                val trackingViewModel: OrderTrackingViewModel = viewModel(
                    key = "tracking-$trackId",
                    factory = OrderTrackingViewModel.Factory(trackId, ordersRepository, orderLocationRepository, authRepository, context)
                )
                val trackingState by trackingViewModel.uiState.collectAsState()
                OrderTrackingScreen(
                    orderId = trackId,
                    uiState = trackingState,
                    onBack = { trackingOrderId = null },
                    light = isLight,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is HomeContent.CourierDetail -> {
                val orderId = content.orderId
                val courierOrderDetailViewModel: CourierOrderDetailViewModel = viewModel(
                    key = "courier-order-detail-$orderId",
                    factory = CourierOrderDetailViewModel.Factory(orderId, ordersRepository, orderFeedRepository, context)
                )
                val courierOrderDetailState by courierOrderDetailViewModel.uiState.collectAsState()
                val courierOrderDetailMessage by courierOrderDetailViewModel.message.collectAsState()

                // The ViewModel is cached in the Activity's ViewModelStore by order
                // id, so re-opening the same order's detail (e.g. after taking it
                // from the list) would otherwise keep showing the stale snapshot
                // from the first visit instead of re-fetching current status.
                LaunchedEffect(orderId) {
                    courierOrderDetailViewModel.load()
                }

                CourierOrderDetailScreen(
                    uiState = courierOrderDetailState,
                    message = courierOrderDetailMessage,
                    onConsumeMessage = courierOrderDetailViewModel::consumeMessage,
                    onBack = { selectedOrderId = null },
                    onTakeOrder = courierOrderDetailViewModel::takeOrder,
                    onMarkInDelivery = courierOrderDetailViewModel::markInDelivery,
                    onReleaseOrder = courierOrderDetailViewModel::releaseOrder,
                    onCollectPayment = courierOrderDetailViewModel::collectPayment,
                    onOpenChat = { chatOrderId = orderId },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is HomeContent.ClientDetail -> {
                val orderId = content.orderId
                val orderDetailViewModel: OrderDetailViewModel = viewModel(
                    key = "order-detail-$orderId",
                    factory = OrderDetailViewModel.Factory(orderId, ordersRepository, profileRepository, catalogRepository, orderFeedRepository, context)
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
                    onOpenChat = { chatOrderId = orderId },
                    onOpenTracking = { trackingOrderId = orderId },
                    light = isLight,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            HomeContent.Tabs -> {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                // Bottom-tab switches are lateral, not forward/back — a
                // quick directional nudge plus crossfade reads as "moved
                // sideways" without the heavier forward-navigation feel above.
                val forward = targetState > initialState
                (slideInHorizontally(animationSpec = tween(200), initialOffsetX = { if (forward) it / 6 else -it / 6 }) + fadeIn(animationSpec = tween(200))) togetherWith
                    fadeOut(animationSpec = tween(120))
            },
            label = "tabContent"
        ) { tab ->
        if (isCourier) {
        when (tab) {
            0 -> {
                val courierOrdersViewModel: CourierOrdersViewModel = viewModel(
                    factory = CourierOrdersViewModel.Factory(ordersRepository, profileRepository, orderFeedRepository, context)
                )
                val uiState by courierOrdersViewModel.uiState.collectAsState()
                val isRefreshing by courierOrdersViewModel.isRefreshing.collectAsState()
                CourierOrdersScreen(
                    uiState = uiState,
                    isRefreshing = isRefreshing,
                    onRefresh = courierOrdersViewModel::refresh,
                    onToggleAvailability = courierOrdersViewModel::toggleAvailability,
                    onTakeOrder = courierOrdersViewModel::takeOrder,
                    onOrderClick = { order -> selectedOrderId = order.id },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            1 -> {
                ProfileTab(
                    profileRepository = profileRepository,
                    catalogRepository = catalogRepository,
                    locationProvider = locationProvider,
                    context = context,
                    innerPadding = innerPadding,
                    onLogoutClick = onLogoutClick,
                    light = true
                )
            }
        }
        } else if (isPartner) {
        when (tab) {
            0 -> {
                val partnerOrdersViewModel: PartnerOrdersViewModel = viewModel(
                    factory = PartnerOrdersViewModel.Factory(ordersRepository, profileRepository, orderFeedRepository, context)
                )
                val uiState by partnerOrdersViewModel.uiState.collectAsState()
                val isRefreshing by partnerOrdersViewModel.isRefreshing.collectAsState()
                PartnerOrdersScreen(
                    uiState = uiState,
                    isRefreshing = isRefreshing,
                    onRefresh = partnerOrdersViewModel::refresh,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            1 -> {
                ProfileTab(
                    profileRepository = profileRepository,
                    catalogRepository = catalogRepository,
                    locationProvider = locationProvider,
                    context = context,
                    innerPadding = innerPadding,
                    onLogoutClick = onLogoutClick,
                    light = true
                )
            }
        }
        } else {
        when (tab) {
            0 -> {
                var selectedService by remember { mutableStateOf<ServiceDto?>(null) }
                // Bumped every time a service is opened so re-opening the same
                // service after an order was created gets a fresh ViewModel
                // instead of one whose orderCreated flag is still true.
                var orderFormToken by remember { mutableIntStateOf(0) }
                var selectedMerchantId by remember { mutableStateOf<Int?>(null) }
                val service = selectedService
                val merchantId = selectedMerchantId

                val servicesTabContent: ServicesTabContent = when {
                    merchantId != null -> ServicesTabContent.Merchant(merchantId)
                    service != null -> ServicesTabContent.Order(service, orderFormToken)
                    else -> ServicesTabContent.Catalog
                }
                if (merchantId != null) {
                    BackHandler { selectedMerchantId = null }
                } else if (service != null) {
                    BackHandler { selectedService = null }
                }

                AnimatedContent(
                    targetState = servicesTabContent,
                    transitionSpec = {
                        val enteringDetail = targetState !is ServicesTabContent.Catalog && initialState is ServicesTabContent.Catalog
                        val leavingDetail = targetState is ServicesTabContent.Catalog && initialState !is ServicesTabContent.Catalog
                        when {
                            enteringDetail -> (slideInHorizontally(animationSpec = tween(260), initialOffsetX = { it }) + fadeIn(animationSpec = tween(220))) togetherWith
                                fadeOut(animationSpec = tween(140))
                            leavingDetail -> fadeIn(animationSpec = tween(180)) togetherWith
                                (slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { it }) + fadeOut(animationSpec = tween(180)))
                            else -> fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                        }
                    },
                    label = "servicesTabContent"
                ) { content ->
                when (content) {
                    is ServicesTabContent.Merchant -> {
                        val mId = content.merchantId
                        val merchantDetailViewModel: MerchantDetailViewModel = viewModel(
                            key = "merchant-$mId",
                            factory = MerchantDetailViewModel.Factory(mId, catalogRepository, cartRepository, context)
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
                            light = isLight,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    is ServicesTabContent.Order -> {
                        val svc = content.service
                        val createOrderViewModel: CreateOrderViewModel = viewModel(
                            key = "${svc.id}-${content.formToken}",
                            factory = CreateOrderViewModel.Factory(
                                catalogRepository = catalogRepository,
                                profileRepository = profileRepository,
                                ordersRepository = ordersRepository,
                                locationProvider = locationProvider,
                                context = context
                            )
                        )
                        val orderUiState by createOrderViewModel.uiState.collectAsState()
                        val orderCreated by createOrderViewModel.orderCreated.collectAsState()
                        val addingCurrentLocation by createOrderViewModel.addingCurrentLocation.collectAsState()

                        LaunchedEffect(orderCreated) {
                            if (orderCreated) {
                                selectedService = null
                            }
                        }

                        ServiceOrderScreen(
                            service = svc,
                            uiState = orderUiState,
                            onBack = { selectedService = null },
                            onPickupManualAddressChange = createOrderViewModel::updatePickupManualAddress,
                            onLocationSelect = createOrderViewModel::selectLocation,
                            onManualEntrySelect = createOrderViewModel::selectManualEntry,
                            onManualAddressChange = createOrderViewModel::updateManualAddress,
                            onCommentChange = createOrderViewModel::updateComment,
                            onCurrencySelect = createOrderViewModel::selectCurrency,
                            onSubmit = { createOrderViewModel.submit(svc.id, svc.kind) },
                            onRequestCurrentLocation = createOrderViewModel::addCurrentLocation,
                            isAddingCurrentLocation = addingCurrentLocation,
                            onCurrentLocationPermissionDenied = createOrderViewModel::locationPermissionDenied,
                            message = (orderUiState as? CreateOrderUiState.Ready)?.message,
                            onConsumeMessage = createOrderViewModel::consumeMessage,
                            light = isLight,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    ServicesTabContent.Catalog -> {
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
                            light = isLight,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
                }
            }

            1 -> {
                var selectedMerchantId by remember { mutableStateOf<Int?>(null) }
                val merchantId = selectedMerchantId
                val shopTabContent: ShopTabContent =
                    if (merchantId != null) ShopTabContent.Merchant(merchantId) else ShopTabContent.Catalog
                if (merchantId != null) {
                    BackHandler { selectedMerchantId = null }
                }

                AnimatedContent(
                    targetState = shopTabContent,
                    transitionSpec = {
                        val enteringDetail = targetState !is ShopTabContent.Catalog && initialState is ShopTabContent.Catalog
                        val leavingDetail = targetState is ShopTabContent.Catalog && initialState !is ShopTabContent.Catalog
                        when {
                            enteringDetail -> (slideInHorizontally(animationSpec = tween(260), initialOffsetX = { it }) + fadeIn(animationSpec = tween(220))) togetherWith
                                fadeOut(animationSpec = tween(140))
                            leavingDetail -> fadeIn(animationSpec = tween(180)) togetherWith
                                (slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { it }) + fadeOut(animationSpec = tween(180)))
                            else -> fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                        }
                    },
                    label = "shopTabContent"
                ) { content ->
                when (content) {
                    ShopTabContent.Catalog -> {
                        val shopViewModel: ShopViewModel = viewModel(
                            factory = ShopViewModel.Factory(catalogRepository, profileRepository, context)
                        )
                        val uiState by shopViewModel.uiState.collectAsState()
                        val isRefreshing by shopViewModel.isRefreshing.collectAsState()
                        ShopScreen(
                            uiState = uiState,
                            isRefreshing = isRefreshing,
                            isGridView = isShopGridView,
                            onToggleGridView = {
                                isShopGridView = !isShopGridView
                                shopDisplayPrefs.isGridView = isShopGridView
                            },
                            onRefresh = shopViewModel::refresh,
                            onMerchantClick = { merchant -> selectedMerchantId = merchant.id },
                            light = isLight,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    is ShopTabContent.Merchant -> {
                        val mId = content.merchantId
                        val merchantDetailViewModel: MerchantDetailViewModel = viewModel(
                            key = "merchant-$mId",
                            factory = MerchantDetailViewModel.Factory(mId, catalogRepository, cartRepository, context)
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
                            light = isLight,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
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
                    light = isLight,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            3 -> {
                val cartViewModel: CartViewModel = viewModel(
                    factory = CartViewModel.Factory(cartRepository, catalogRepository, ordersRepository, profileRepository, locationProvider, context)
                )
                val uiState by cartViewModel.uiState.collectAsState()
                val orderCreated by cartViewModel.orderCreated.collectAsState()
                val addingCurrentLocation by cartViewModel.addingCurrentLocation.collectAsState()

                // This branch is torn down and rebuilt each time the user leaves
                // and returns to the Cart tab, so this fires on every re-entry —
                // picks up locations added/deleted from Profile in the meantime.
                LaunchedEffect(Unit) {
                    cartViewModel.refreshLocations()
                }

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
                    onClearCart = cartViewModel::clearCart,
                    onLocationSelect = cartViewModel::selectLocation,
                    onManualEntrySelect = cartViewModel::selectManualEntry,
                    onManualAddressChange = cartViewModel::updateManualAddress,
                    onCommentChange = cartViewModel::updateComment,
                    onCurrencySelect = cartViewModel::selectCurrency,
                    onSubmit = cartViewModel::submit,
                    onRequestCurrentLocation = cartViewModel::addCurrentLocation,
                    isAddingCurrentLocation = addingCurrentLocation,
                    onCurrentLocationPermissionDenied = cartViewModel::locationPermissionDenied,
                    message = (uiState as? CartUiState.Ready)?.message,
                    onConsumeMessage = cartViewModel::consumeMessage,
                    light = isLight,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            4 -> {
                ProfileTab(
                    profileRepository = profileRepository,
                    catalogRepository = catalogRepository,
                    locationProvider = locationProvider,
                    context = context,
                    innerPadding = innerPadding,
                    onLogoutClick = onLogoutClick,
                    light = true
                )
            }
        }
        }
        }
            }
        }
        }
    }
}

// Shared between the client's Профиль tab (index 4) and the courier's
// Профиль tab (index 1) — same account/profile screen for every role.
@Composable
private fun ProfileTab(
    profileRepository: ProfileRepository,
    catalogRepository: CatalogRepository,
    locationProvider: LocationProvider,
    context: Context,
    innerPadding: PaddingValues,
    onLogoutClick: () -> Unit,
    light: Boolean = false
) {
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
        light = light,
        modifier = Modifier.padding(innerPadding)
    )
}
