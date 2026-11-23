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

    @Test
    fun `solo el operador edita y solo si no esta validada`() {
        assertTrue(Permisos.puedeEditar(Rol.OPERADOR, EstadoRevision.PENDIENTE))
        assertTrue(Permisos.puedeEditar(Rol.OPERADOR, EstadoRevision.OBSERVADO))
        assertFalse(Permisos.puedeEditar(Rol.OPERADOR, EstadoRevision.VALIDADO))
        assertFalse(Permisos.puedeEditar(Rol.ANALISTA, EstadoRevision.PENDIENTE))
        assertFalse(Permisos.puedeEditar(null, EstadoRevision.PENDIENTE))
    }

    @Test
    fun `el analista ve solo validadas`() {
        assertTrue(Permisos.veSoloValidadas(Rol.ANALISTA))
        assertFalse(Permisos.veSoloValidadas(Rol.OPERADOR))
        assertFalse(Permisos.veSoloValidadas(null))
    }
}
