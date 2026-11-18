package com.duoc.aquasample.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MuestraTest {

    @Test
    fun `la cantidad de choritos es la cantidad de marcas`() {
        assertEquals(2, muestraDePrueba().cantidadChoritos)
        assertEquals(0, muestraDePrueba(choritos = emptyList()).cantidadChoritos)
    }

    @Test
    fun `el tamaño promedio ignora los choritos sin medir`() {
        val muestra = muestraDePrueba(
            choritos = listOf(
                Chorito(numero = 1, tamanoCm = 4.0),
                Chorito(numero = 2, tamanoCm = 6.0),
                Chorito(numero = 3, tamanoCm = null)
            )
        )
        assertEquals(5.0, muestra.tamanoPromedioCm!!, 0.0001)
    }

    @Test
    fun `sin mediciones el tamaño promedio es null`() {
        val muestra = muestraDePrueba(choritos = listOf(Chorito(numero = 1)))
        assertNull(muestra.tamanoPromedioCm)
    }

    @Test
    fun `el codigo se rellena con ceros a 4 digitos`() {
        assertEquals("#0007", muestraDePrueba(id = 7).codigo)
        assertEquals("#0123", muestraDePrueba(id = 123).codigo)
    }

    @Test
    fun `una muestra nueva parte pendiente de revision y de sincronizacion`() {
        val muestra = muestraDePrueba()
        assertEquals(EstadoRevision.PENDIENTE, muestra.estado)
        assertEquals(EstadoSync.PENDIENTE, muestra.estadoSync)
    }
}
