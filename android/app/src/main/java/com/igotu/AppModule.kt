package com.igotu

import com.igotu.data.ApiService
import com.igotu.data.TokenStore
import com.igotu.messaging.WhatsAppProvider
import com.igotu.voice.OnDeviceVoiceTriggerProvider
import com.igotu.voice.VoiceTriggerProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {
    @Binds
    abstract fun bindVoiceTriggerProvider(impl: OnDeviceVoiceTriggerProvider): VoiceTriggerProvider
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttp(tokenStore: TokenStore): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .addInterceptor { chain ->
            val request = tokenStore.token?.let { token ->
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } ?: chain.request()
            chain.proceed(request)
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://igotu.onrender.com/v1/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
