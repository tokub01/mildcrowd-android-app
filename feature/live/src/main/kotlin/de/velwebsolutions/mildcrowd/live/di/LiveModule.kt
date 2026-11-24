package de.velwebsolutions.mildcrowd.live.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.velwebsolutions.mildcrowd.live.data.DefaultLiveRepository
import de.velwebsolutions.mildcrowd.live.data.LiveRepository
import de.velwebsolutions.mildcrowd.live.push.AndroidPushSystem
import de.velwebsolutions.mildcrowd.live.push.PreferencesPushStore
import de.velwebsolutions.mildcrowd.live.push.PushNotifications
import de.velwebsolutions.mildcrowd.live.push.PushRegistrar
import de.velwebsolutions.mildcrowd.live.push.PushSystem
import javax.inject.Singleton

/** The Live module's objects: the repository and push on this device. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LiveModule {
    @Binds
    @Singleton
    abstract fun repository(repository: DefaultLiveRepository): LiveRepository

    companion object {
        @Provides
        @Singleton
        fun pushSystem(@ApplicationContext context: Context): PushSystem {
            PushNotifications.ensureChannel(context)
            return AndroidPushSystem(context)
        }

        @Provides
        @Singleton
        fun pushRegistrar(system: PushSystem, @ApplicationContext context: Context): PushRegistrar =
            PushRegistrar(system, PreferencesPushStore(context))
    }
}
