package com.healthsync.ai.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.healthsync.ai.data.remote.eightsleep.EightSleepApiService
import com.healthsync.ai.data.remote.eightsleep.EightSleepAuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    @Named("eightSleepAuth")
    fun provideEightSleepAuthRetrofit(client: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://auth-api.8slp.net/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    @Named("eightSleepClient")
    fun provideEightSleepClientRetrofit(client: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://client-api.8slp.net/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideEightSleepAuthService(@Named("eightSleepAuth") retrofit: Retrofit): EightSleepAuthService {
        return retrofit.create(EightSleepAuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideEightSleepApiService(@Named("eightSleepClient") retrofit: Retrofit): EightSleepApiService {
        return retrofit.create(EightSleepApiService::class.java)
    }
}
