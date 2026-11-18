package com.duoc.aquasample.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.duoc.aquasample.AquaSampleApplication

/**
 * Trabajo en segundo plano de WorkManager: corre aunque la app esté cerrada
 * y solo cuando hay red (ver ProgramadorSincronizacion). Si no se pudo
 * terminar, pide reintento y WorkManager lo vuelve a lanzar más tarde.
 */
class SincronizacionWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as AquaSampleApplication).container
        return when (container.sincronizador.sincronizarPendientes()) {
            is ResultadoSync.Completo -> Result.success()
            is ResultadoSync.Reintentar -> Result.retry()
        }
    }
}
