package com.duoc.aquasample

import android.app.Application
import com.duoc.aquasample.data.AppContainer

/** Registrada en AndroidManifest.xml (android:name). Vive mientras viva la app. */
class AquaSampleApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Al abrir la app: sube lo que haya quedado pendiente y deja
        // programada la sincronización periódica.
        container.programadorSincronizacion.sincronizarPronto()
        container.programadorSincronizacion.programarPeriodica()
    }
}
