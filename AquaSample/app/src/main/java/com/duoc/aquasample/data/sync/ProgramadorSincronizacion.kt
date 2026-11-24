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
        // REPLACE y no APPEND: si un intento anterior falló (por ejemplo, sin
        // backend), WorkManager lo deja esperando con espera exponencial
        // (30 s, 1 min, 2 min... hasta horas). Con APPEND, cada guardado y cada
        // toque de "Sincronizar" quedaba en fila detrás de ese intento dormido.
        // Reemplazarlo es seguro: el trabajo nuevo vuelve a leer todas las
        // pendientes de Room y el backend reconoce cada muestra por su uuid.
        workManager.enqueueUniqueWork(TRABAJO_INMEDIATO, ExistingWorkPolicy.REPLACE, trabajo)
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
