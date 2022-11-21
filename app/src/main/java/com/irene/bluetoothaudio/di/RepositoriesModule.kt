package com.irene.bluetoothaudio.di

import android.content.Context
import com.irene.bluetoothaudio.bluetooth_repository.BluetoothRepository
import com.irene.bluetoothaudio.bluetooth_service.BluetoothService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RepositoriesModule {

    @Provides
    @Singleton
    fun provideBluetoothRepository(@ApplicationContext context: Context,
                                   bluetoothService: BluetoothService
    ): BluetoothRepository = BluetoothRepository(context, bluetoothService)

}