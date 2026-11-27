package de.velwebsolutions.mildcrowd.membership.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.velwebsolutions.mildcrowd.core.auth.Session
import de.velwebsolutions.mildcrowd.membership.billing.BillingClientConnection
import de.velwebsolutions.mildcrowd.membership.billing.BillingGateway
import de.velwebsolutions.mildcrowd.membership.billing.PlayBillingConnection
import de.velwebsolutions.mildcrowd.membership.billing.PlayBillingGateway
import de.velwebsolutions.mildcrowd.membership.data.DefaultMembershipRepository
import de.velwebsolutions.mildcrowd.membership.data.MembershipFiles
import de.velwebsolutions.mildcrowd.membership.data.MembershipRepository
import javax.inject.Singleton

/** The membership's repository, Google Play Billing and the server check of Play purchases. */
@Module
@InstallIn(SingletonComponent::class)
abstract class MembershipModule {
    /** The repository the screens use. */
    @Binds
    abstract fun repository(repository: DefaultMembershipRepository): MembershipRepository

    /** Google Play Billing for the checkout. */
    @Binds
    abstract fun billing(gateway: PlayBillingGateway): BillingGateway

    /** Play's client behind the billing gateway. */
    @Binds
    abstract fun billingConnection(connection: BillingClientConnection): PlayBillingConnection

    companion object {
        /** Invoices and exports: leftovers of an earlier process go at once, all of them when the member signs out. */
        @Provides
        @Singleton
        fun files(@ApplicationContext context: Context, session: Session): MembershipFiles {
            val files = MembershipFiles(context.cacheDir)
            files.clear()
            session.addOnSignedOut(files::clear)
            return files
        }
    }
}
