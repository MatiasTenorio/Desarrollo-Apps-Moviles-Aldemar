package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SesionRepositoryTest {

    @Test
    fun `parte sin usuario, guarda el que inicia y lo borra al cerrar`() {
        val sesion = SesionRepository()
        assertNull(sesion.usuario.value)

        val usuario = Usuario("operador1", "1234", Rol.OPERADOR)
        sesion.iniciar(usuario)
        assertEquals(usuario, sesion.usuario.value)

        sesion.cerrar()
        assertNull(sesion.usuario.value)
    }
}
