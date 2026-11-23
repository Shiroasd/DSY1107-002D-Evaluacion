package com.retail.inventory.controller;

import com.retail.inventory.domain.AlertaInventario;
import com.retail.inventory.exception.BadRequestException;
import com.retail.inventory.repository.AlertaInventarioRepository;
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
public class AlertaInventarioController {
    private final AlertaInventarioRepository repository;

    @GetMapping("/api/v1/alertas")
    public PagedModel<AlertaInventario> listar(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Use page >= 0 y size entre 1 y 100");
        }
        var sort = Sort.by(Sort.Direction.DESC, "fecha", "eventId");
        return new PagedModel<>(repository.findAll(PageRequest.of(page, size, sort)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Parametro invalido: " + error.getName());
    }
}
