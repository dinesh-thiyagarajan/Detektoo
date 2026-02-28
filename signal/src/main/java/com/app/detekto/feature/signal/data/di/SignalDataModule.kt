package com.app.detekto.feature.signal.data.di

import com.app.detekto.feature.signal.data.repository.SignalRepositoryImpl
import com.app.detekto.feature.signal.domain.repository.SignalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SignalDataModule {

    @Binds
    @Singleton
    abstract fun bindSignalRepository(impl: SignalRepositoryImpl): SignalRepository
}
