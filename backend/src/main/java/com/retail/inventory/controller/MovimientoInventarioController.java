package com.retail.inventory.controller;

import com.retail.inventory.domain.MovimientoInventario;
import com.retail.inventory.exception.BadRequestException;
import com.retail.inventory.service.MovimientoInventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MovimientoInventarioController {
    private final MovimientoInventarioService service;

    @GetMapping("/api/v1/movimientos")
    public PagedModel<MovimientoInventario> listar(
        @RequestParam(required = false) Long productoId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "desc") String orden
    ) {
        if (page < 0 || size < 1 || size > 100 || (productoId != null && productoId < 1)
            || !("asc".equalsIgnoreCase(orden) || "desc".equalsIgnoreCase(orden))) {
            throw new BadRequestException("Use page >= 0, size entre 1 y 100, productoId positivo y orden asc o desc");
        }
        Sort sort = Sort.by(Sort.Direction.fromString(orden), "fecha", "eventId");
        return new PagedModel<>(service.consultar(productoId, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/api/v1/products/{id}/movimientos")
    public PagedModel<MovimientoInventario> listarPorProducto(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "desc") String orden
    ) {
        return listar(id, page, size, orden);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Parametro invalido: " + error.getName());
    }
}
