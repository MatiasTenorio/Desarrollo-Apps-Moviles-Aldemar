package com.duoc.aquasample.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.duoc.aquasample.AquaSampleApplication
import com.duoc.aquasample.data.AppContainer

/**
 * Atajo para las Factory de los ViewModels: entrega el AppContainer de la
 * app (repositorios compartidos). Uso dentro de viewModelFactory { initializer { ... } }:
 *     MiViewModel(appContainer().muestraRepository)
 */
fun CreationExtras.appContainer(): AppContainer =
    (this[APPLICATION_KEY] as AquaSampleApplication).container
