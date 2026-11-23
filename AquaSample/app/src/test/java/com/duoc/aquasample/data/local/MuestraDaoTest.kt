package com.duoc.aquasample.data.local

import com.duoc.aquasample.data.local.dao.MuestraDao
import com.duoc.aquasample.data.local.entity.ChoritoEntity
import com.duoc.aquasample.data.model.muestraDePrueba
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * guardarConChoritos es la única lógica escrita a mano en el DAO (el resto lo
 * genera Room). Se prueba llamando al método real sobre un DAO simulado.
 */
class MuestraDaoTest {

    private val zona = ZoneId.of("America/Santiago")
    private val dao = mockk<MuestraDao>(relaxed = true).also {
        coEvery { it.guardarConChoritos(any(), any()) } coAnswers { callOriginal() }
    }
    private val choritos = listOf(
        ChoritoEntity(id = 99, muestraId = 0, numero = 1, posX = 0.1f, posY = 0.2f, tamanoCm = 5.0)
    )

    @Test
    fun `una muestra nueva se inserta y sus choritos quedan con el id nuevo`() = runTest {
        coEvery { dao.insertarMuestra(any()) } returns 10L

        val id = dao.guardarConChoritos(muestraDePrueba(id = 0).aEntidad(0L, zona), choritos)

        assertEquals(10L, id)
        coVerify(exactly = 0) { dao.actualizarMuestra(any()) }
        coVerifyOrder {
            dao.borrarChoritosDe(10L)
            dao.insertarChoritos(listOf(choritos.single().copy(id = 0, muestraId = 10L)))
        }
    }

    @Test
    fun `una muestra existente se actualiza y se reemplazan sus choritos`() = runTest {
        val id = dao.guardarConChoritos(muestraDePrueba(id = 7).aEntidad(0L, zona), choritos)

        assertEquals(7L, id)
        coVerify(exactly = 0) { dao.insertarMuestra(any()) }
        coVerify { dao.actualizarMuestra(match { it.id == 7L }) }
        coVerify { dao.borrarChoritosDe(7L) }
    }

    @Test
    fun `sin choritos no se insertan filas vacias`() = runTest {
        dao.guardarConChoritos(muestraDePrueba(id = 7).aEntidad(0L, zona), emptyList())

        coVerify { dao.borrarChoritosDe(7L) }
        coVerify(exactly = 0) { dao.insertarChoritos(any()) }
    }
}
