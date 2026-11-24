package com.duoc.aquasample_api.repository;

import com.duoc.aquasample_api.model.FotoMuestra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FotoMuestraRepository extends JpaRepository<FotoMuestra, String> {
}
