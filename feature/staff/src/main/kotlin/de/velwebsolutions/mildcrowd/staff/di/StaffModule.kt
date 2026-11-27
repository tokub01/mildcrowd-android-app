package de.velwebsolutions.mildcrowd.staff.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.velwebsolutions.mildcrowd.core.auth.Session
import de.velwebsolutions.mildcrowd.staff.data.DefaultStaffRepository
import de.velwebsolutions.mildcrowd.staff.data.StaffFiles
import de.velwebsolutions.mildcrowd.staff.data.StaffRepository
import javax.inject.Singleton

/** The staff repository and the evidence files. */
@Module
@InstallIn(SingletonComponent::class)
abstract class StaffModule {
    /** The repository the screens use. */
    @Binds
    abstract fun repository(repository: DefaultStaffRepository): StaffRepository

    companion object {
        /**
         * Evidence archives and reported files: leftovers of an earlier process go when they are first needed, and from
         * then on all of them whenever the member signs out (also when the token is revoked).
         */
        @Provides
        @Singleton
        fun files(@ApplicationContext context: Context, session: Session): StaffFiles {
            val files = StaffFiles(context.cacheDir)
            files.clear()
            session.addOnSignedOut(files::clear)
            return files
        }
    }
}
