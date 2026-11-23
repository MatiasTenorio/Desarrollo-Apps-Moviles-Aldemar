package com.duoc.aquasample.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermisosTest {

    @Test
    fun `el operador registra y valida`() {
        assertTrue(Permisos.puedeRegistrarMuestras(Rol.OPERADOR))
        assertTrue(Permisos.puedeCambiarEstado(Rol.OPERADOR))
    }

    @Test
    fun `el analista solo consulta`() {
        assertFalse(Permisos.puedeRegistrarMuestras(Rol.ANALISTA))
        assertFalse(Permisos.puedeCambiarEstado(Rol.ANALISTA))
    }

    @Test
    fun `sin sesion no se permite nada`() {
        assertFalse(Permisos.puedeRegistrarMuestras(null))
        assertFalse(Permisos.puedeCambiarEstado(null))
    }
}
