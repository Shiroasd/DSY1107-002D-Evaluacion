package com.retail.inventory.controller;

import com.retail.inventory.exception.BadRequestException;
import com.retail.inventory.service.AnaliticaInventarioService;
import com.retail.inventory.service.AnaliticaInventarioService.Producto;
import com.retail.inventory.service.AnaliticaInventarioService.Resumen;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics/inventory")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class AnaliticaInventarioController {
    private final AnaliticaInventarioService service;

    @GetMapping("/summary")
    public Resumen resumen() {
        return service.resumen();
    }

    @GetMapping("/products")
    public PagedModel<Producto> productos(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        if (page < 0) {
            throw new BadRequestException("page debe ser mayor o igual a cero");
        }
        validarLimite(size);
        return new PagedModel<>(service.productos(PageRequest.of(page, size, Sort.by("id"))));
    }

    @GetMapping("/top-products")
    public List<Producto> productosMasActivos(@RequestParam(defaultValue = "5") int limit) {
        validarLimite(limit);
        var sort = Sort.by(Sort.Order.desc("movimientos"), Sort.Order.asc("id"));
        return service.productos(PageRequest.of(0, limit, sort)).getContent();
    }

    private void validarLimite(int limite) {
        if (limite < 1 || limite > 100) {
            throw new BadRequestException("El limite debe estar entre 1 y 100");
        }
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Parametro invalido: " + error.getName());
    }
}
