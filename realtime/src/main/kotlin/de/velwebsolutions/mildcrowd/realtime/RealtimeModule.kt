package de.velwebsolutions.mildcrowd.realtime

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.velwebsolutions.mildcrowd.core.network.ApiClient
import de.velwebsolutions.mildcrowd.core.network.RealtimeConfig
import de.velwebsolutions.mildcrowd.core.realtime.RealtimeService
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * The realtime service of the app: Reverb with the settings of `app-config` (the host replaced by
 * [RealtimeConfig.hostOverride] in the emulator) and channel authorisation with the API token. It connects while a
 * screen listens and reconnects at once when the app returns to the foreground.
 */
@Module
@InstallIn(SingletonComponent::class)
object RealtimeModule {
    @Provides
    @Singleton
    fun realtime(client: ApiClient, config: RealtimeConfig): RealtimeService {
        val api = RealtimeApi(client)
        // A plain client: the API token goes to the API only, never to the WebSocket host.
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        val reverb = ReverbRealtime(
            settings = { api.settings()?.withHost(config.hostOverride) },
            authorize = api::authorize,
            makeSocket = { url -> OkHttpWebSocketConnection(okHttp, url) }
        )
        Handler(Looper.getMainLooper()).post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_START) reverb.reconnectIfNeeded() }
            )
        }
        return reverb
    }
}
