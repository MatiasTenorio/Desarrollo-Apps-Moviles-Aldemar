package com.duoc.aquasample.data.model

/**
 * Qué puede hacer cada rol. Decisión del equipo (Evidencia de Clase 1): el
 * Operador de muestreo y el Supervisor técnico se fusionaron en un solo rol
 * Operador, que registra muestras y también las revisa y valida. El Analista
 * solo consulta: historial, filtros, detalle, dashboard y reportes.
 * Sin sesión (rol null) no se permite nada.
 */
object Permisos {

    fun puedeRegistrarMuestras(rol: Rol?): Boolean = rol == Rol.OPERADOR

    fun puedeCambiarEstado(rol: Rol?): Boolean = rol == Rol.OPERADOR

    /** Una muestra validada queda cerrada: para corregirla, el Operador primero la cambia a Observado. */
    fun puedeEditar(rol: Rol?, estado: EstadoRevision): Boolean =
        rol == Rol.OPERADOR && estado != EstadoRevision.VALIDADO

    /** El Analista revisa los registros validados de todos los operadores. */
    fun veSoloValidadas(rol: Rol?): Boolean = rol == Rol.ANALISTA
}
