package com.duoc.aquasample_api.service;

import com.duoc.aquasample_api.dto.MuestraResponse;

/** Resultado de guardar por uuid: la muestra y si fue creada (true) o actualizada (false). */
public record ResultadoGuardado(MuestraResponse muestra, boolean creada) {
}
