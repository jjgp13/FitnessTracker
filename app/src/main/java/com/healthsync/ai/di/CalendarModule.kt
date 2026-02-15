package com.healthsync.ai.di

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CalendarModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    @Named("calendar")
    fun provideCalendarSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("healthsync_calendar_prefs", Context.MODE_PRIVATE)
    }
}
