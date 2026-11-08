package com.romdoctor.app

import android.content.Context
import com.romdoctor.core.shell.CapabilityDetector
import com.romdoctor.core.shell.ProcessShellExecutor
import com.romdoctor.core.shell.ShellExecutor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideShell(): ShellExecutor = ProcessShellExecutor()

    @Provides
    @Singleton
    fun provideDetector(@ApplicationContext context: Context, shell: ShellExecutor): CapabilityDetector =
        CapabilityDetector(context, shell)
}
