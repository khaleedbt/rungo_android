package dev.batipy.rungo

import android.app.Application
import dev.batipy.rungo.data.auth.AuthRepository
import dev.batipy.rungo.data.auth.TokenStore
import dev.batipy.rungo.data.cart.CartRepository
import dev.batipy.rungo.data.catalog.CatalogRepository
import dev.batipy.rungo.data.chat.ChatRepository
import dev.batipy.rungo.data.location.LocationProvider
import dev.batipy.rungo.data.network.NetworkModule
import dev.batipy.rungo.data.notifications.NotificationRepository
import dev.batipy.rungo.data.notifications.createOrderNotificationChannel
import dev.batipy.rungo.data.orders.OrderFeedRepository
import dev.batipy.rungo.data.orders.OrdersRepository
import dev.batipy.rungo.data.profile.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class RunGoApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    val tokenStore by lazy { TokenStore(this) }
    private val api by lazy { NetworkModule.createApi(tokenStore) }
    val authRepository by lazy { AuthRepository(api, tokenStore, this) }
    val catalogRepository by lazy { CatalogRepository(api) }
    val ordersRepository by lazy { OrdersRepository(api) }
    val profileRepository by lazy { ProfileRepository(api) }
    val locationProvider by lazy { LocationProvider(this) }
    val notificationRepository by lazy { NotificationRepository(api) }
    val cartRepository by lazy { CartRepository() }
    val chatRepository by lazy { ChatRepository(tokenStore) }
    val orderFeedRepository by lazy { OrderFeedRepository(tokenStore, applicationScope) }

    override fun onCreate() {
        super.onCreate()
        createOrderNotificationChannel(this)
    }
}
