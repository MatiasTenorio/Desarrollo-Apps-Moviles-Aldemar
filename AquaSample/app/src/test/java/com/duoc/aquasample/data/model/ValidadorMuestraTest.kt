package com.duoc.aquasample.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorMuestraTest {

    private fun errores(muestra: Muestra) = ValidadorMuestra.validar(muestra)

    @Test
    fun `una muestra completa no tiene errores`() {
        assertTrue(errores(muestraDePrueba()).isEmpty())
        assertTrue(ValidadorMuestra.esValida(muestraDePrueba()))
    }

    @Test
    fun `exige centro, tren y operador`() {
        val muestra = muestraDePrueba().copy(centro = " ", tren = "", operador = "")
        val lista = errores(muestra)
        assertTrue(lista.contains("Selecciona el centro de cultivo"))
        assertTrue(lista.contains("Selecciona el tren"))
        assertTrue(lista.contains("Falta el operador"))
        assertFalse(ValidadorMuestra.esValida(muestra))
    }

    @Test
    fun `la linea debe ser positiva`() {
        assertEquals(listOf("Selecciona una línea válida"), errores(muestraDePrueba(linea = 0)))
    }

    @Test
    fun `la longitud del tramo debe ser mayor a cero`() {
        assertEquals(
            listOf("La longitud del tramo debe ser mayor a 0"),
            errores(muestraDePrueba().copy(longitudTramoM = 0.0))
        )
    }

    @Test
    fun `la foto es obligatoria`() {
        assertEquals(listOf("Agrega la foto de la muestra"), errores(muestraDePrueba().copy(fotoPath = null)))
        assertEquals(listOf("Agrega la foto de la muestra"), errores(muestraDePrueba().copy(fotoPath = "  ")))
    }

    @Test
    fun `las observaciones tienen un maximo de caracteres`() {
        val justo = "a".repeat(ValidadorMuestra.MAX_OBSERVACIONES)
        assertTrue(errores(muestraDePrueba().copy(observaciones = justo)).isEmpty())

        val largo = "a".repeat(ValidadorMuestra.MAX_OBSERVACIONES + 1)
        assertEquals(1, errores(muestraDePrueba().copy(observaciones = largo)).size)
    }

    @Test
    fun `un tamaño cero o negativo es invalido, pero sin medir es valido`() {
        val conCero = muestraDePrueba(choritos = listOf(Chorito(numero = 3, tamanoCm = 0.0)))
        assertEquals(listOf("El tamaño del chorito N° 3 debe ser mayor a 0"), errores(conCero))

        val sinMedir = muestraDePrueba(choritos = listOf(Chorito(numero = 1, tamanoCm = null)))
        assertTrue(errores(sinMedir).isEmpty())
    }

    @Test
    fun `una marca fuera de la foto es invalida`() {
        val fueraEnX = muestraDePrueba(choritos = listOf(Chorito(numero = 4, posX = 1.2f, posY = 0.5f)))
        assertEquals(listOf("La marca del chorito N° 4 está fuera de la foto"), errores(fueraEnX))

        val fueraEnY = muestraDePrueba(choritos = listOf(Chorito(numero = 5, posX = 0.5f, posY = -0.1f)))
        assertEquals(listOf("La marca del chorito N° 5 está fuera de la foto"), errores(fueraEnY))
    }

    @Test
    fun `los bordes de la foto son validos`() {
        val enBordes = muestraDePrueba(
            choritos = listOf(Chorito(numero = 1, posX = 0f, posY = 1f))
        )
        assertTrue(errores(enBordes).isEmpty())
    }

    @Test
    fun `no puede haber numeros de chorito repetidos`() {
        val repetidos = muestraDePrueba(
            choritos = listOf(Chorito(numero = 1), Chorito(numero = 1))
        )
        assertEquals(listOf("Hay choritos con el número repetido"), errores(repetidos))
    }

    @Test
    fun `no se puede validar una muestra sin choritos`() {
        val vacia = muestraDePrueba(choritos = emptyList()).copy(estado = EstadoRevision.VALIDADO)
        assertEquals(listOf("No se puede validar una muestra sin choritos contados"), errores(vacia))

        // Pendiente y sin choritos sí es válida (todavía no se cuenta).
        assertTrue(errores(muestraDePrueba(choritos = emptyList())).isEmpty())
    }
}
