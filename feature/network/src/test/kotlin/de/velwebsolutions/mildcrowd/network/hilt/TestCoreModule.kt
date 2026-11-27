package de.velwebsolutions.mildcrowd.network.hilt

import androidx.activity.ComponentActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import de.velwebsolutions.mildcrowd.core.auth.Session
import de.velwebsolutions.mildcrowd.core.di.AppScope
import de.velwebsolutions.mildcrowd.core.di.CoreModule
import de.velwebsolutions.mildcrowd.core.network.ApiClient
import de.velwebsolutions.mildcrowd.core.network.CommonApi
import de.velwebsolutions.mildcrowd.core.network.RemoteCommonApi
import de.velwebsolutions.mildcrowd.core.onboarding.OnboardingCardStore
import de.velwebsolutions.mildcrowd.testing.MemoryOnboardingCardStore
import de.velwebsolutions.mildcrowd.testing.StubTransport
import de.velwebsolutions.mildcrowd.testing.testTokenStore
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** The API of Hilt tests: canned answers instead of a server (unknown requests fail like an unreachable server). */
object TestApi {
    val transport = StubTransport()
}

/** `:core`'s objects for Hilt tests: the stubbed client, a signed-out session in memory. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [CoreModule::class])
object TestCoreModule {
    @Provides
    @Singleton
    @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Provides
    @Singleton
    fun session(@AppScope scope: CoroutineScope): Session = Session(testTokenStore(null), "Pixel Test", scope)

    @Provides
    @Singleton
    fun apiClient(): ApiClient = TestApi.transport.client

    @Provides
    @Singleton
    fun commonApi(client: ApiClient): CommonApi = RemoteCommonApi(client)

    @Provides
    @Singleton
    fun onboardingCard(): OnboardingCardStore = MemoryOnboardingCardStore()
}

/** An activity with Hilt's view-model factory, for screens that create their view models themselves. */
@AndroidEntryPoint(ComponentActivity::class)
class NetworkTestActivity : Hilt_NetworkTestActivity()
