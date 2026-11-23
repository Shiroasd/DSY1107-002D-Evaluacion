package com.retail.inventory.service;

import com.retail.inventory.domain.AlertaInventario;
import com.retail.inventory.dto.InventarioAlertaEvent;
import com.retail.inventory.repository.AlertaInventarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertaInventarioService {
    private final AlertaInventarioRepository repository;
    private final EntityManager entityManager;

    @Transactional
    public boolean registrar(InventarioAlertaEvent event) {
        if (repository.existsById(event.eventId())) {
            return false;
        }
        entityManager.persist(new AlertaInventario(event));
        return true;
    }
}
