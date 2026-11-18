package com.duoc.aquasample.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Decide cuándo sincronizar:
 * - sincronizarPronto(): apenas se guarda o cambia una muestra (si no hay red,
 *   WorkManager espera a que vuelva).
 * - programarPeriodica(): cada 15 minutos (el mínimo de Android), como red de
 *   seguridad por si algún intento quedó pendiente.
 */
class ProgramadorSincronizacion(contexto: Context) {

    private val workManager = WorkManager.getInstance(contexto.applicationContext)

    private val conRed = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun sincronizarPronto() {
        val trabajo = OneTimeWorkRequestBuilder<SincronizacionWorker>()
            .setConstraints(conRed)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // APPEND_OR_REPLACE: si ya hay una sincronización corriendo, esta se
        // encola después, así no se pierde una muestra guardada a mitad de camino.
        workManager.enqueueUniqueWork(TRABAJO_INMEDIATO, ExistingWorkPolicy.APPEND_OR_REPLACE, trabajo)
    }

    fun programarPeriodica() {
        val trabajo = PeriodicWorkRequestBuilder<SincronizacionWorker>(15, TimeUnit.MINUTES)
            .setConstraints(conRed)
            .build()
        workManager.enqueueUniquePeriodicWork(TRABAJO_PERIODICO, ExistingPeriodicWorkPolicy.KEEP, trabajo)
    }

    private companion object {
        const val TRABAJO_INMEDIATO = "sincronizar_muestras"
        const val TRABAJO_PERIODICO = "sincronizar_muestras_periodico"
    }
}
