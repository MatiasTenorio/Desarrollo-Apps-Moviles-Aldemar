package com.duoc.aquasample.data.model

/**
 * Reglas de negocio de una muestra, como función pura: recibe una muestra y
 * devuelve la lista de errores (vacía = válida). No depende de Android, así
 * que se prueba con JUnit sin emulador.
 */
object ValidadorMuestra {

    const val MAX_OBSERVACIONES = 500

    fun validar(muestra: Muestra): List<String> = buildList {
        if (muestra.centro.isBlank()) add("Selecciona el centro de cultivo")
        if (muestra.tren.isBlank()) add("Selecciona el tren")
        if (muestra.linea <= 0) add("Selecciona una línea válida")
        if (muestra.longitudTramoM <= 0.0) add("La longitud del tramo debe ser mayor a 0")
        if (muestra.operador.isBlank()) add("Falta el operador")
        // Decisión del equipo: una muestra lleva exactamente una foto.
        if (muestra.fotoPath.isNullOrBlank()) add("Agrega la foto de la muestra")
        if (muestra.observaciones.length > MAX_OBSERVACIONES) {
            add("Las observaciones no pueden superar $MAX_OBSERVACIONES caracteres")
        }

        muestra.choritos
            .firstOrNull { chorito -> chorito.tamanoCm?.let { it <= 0.0 } == true }
            ?.let { add("El tamaño del chorito N° ${it.numero} debe ser mayor a 0") }

        muestra.choritos
            .firstOrNull { !marcaDentroDeLaFoto(it.posX) || !marcaDentroDeLaFoto(it.posY) }
            ?.let { add("La marca del chorito N° ${it.numero} está fuera de la foto") }

        if (muestra.choritos.map { it.numero }.toSet().size != muestra.choritos.size) {
            add("Hay choritos con el número repetido")
        }

        if (muestra.estado == EstadoRevision.VALIDADO && muestra.choritos.isEmpty()) {
            add("No se puede validar una muestra sin choritos contados")
        }
    }

    fun esValida(muestra: Muestra): Boolean = validar(muestra).isEmpty()

    private fun marcaDentroDeLaFoto(valor: Float?): Boolean =
        valor == null || valor in 0f..1f
}
