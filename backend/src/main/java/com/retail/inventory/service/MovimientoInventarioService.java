package com.retail.inventory.service;

import com.retail.inventory.domain.MovimientoInventario;
import com.retail.inventory.dto.InventarioMovimientoEvent;
import com.retail.inventory.repository.MovimientoInventarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioService {
    private final MovimientoInventarioRepository repository;
    private final EntityManager entityManager;

    @Transactional
    public boolean registrar(InventarioMovimientoEvent event) {
        if (repository.existsById(event.eventId())) {
            return false;
        }
        entityManager.persist(new MovimientoInventario(event));
        return true;
    }

    @Transactional(readOnly = true)
    public Page<MovimientoInventario> consultar(Long productoId, Pageable pageable) {
        return productoId == null ? repository.findAll(pageable) : repository.findByProductoId(productoId, pageable);
    }
}
