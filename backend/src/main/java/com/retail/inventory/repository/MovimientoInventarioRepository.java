package com.retail.inventory.repository;

import com.retail.inventory.domain.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, UUID> {
    Page<MovimientoInventario> findByProductoId(Long productoId, Pageable pageable);
}
