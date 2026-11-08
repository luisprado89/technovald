package com.technovald.service.impl;

import com.technovald.dto.response.ProductoMasVendidoResponse;
import com.technovald.repository.mysql.PedidoDetalleRepository;
import com.technovald.service.EstadisticaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Implementación de {@link EstadisticaService}. */
@Service
@RequiredArgsConstructor
@Slf4j
public class EstadisticaServiceImpl implements EstadisticaService {

    private final PedidoDetalleRepository pedidoDetalleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoMasVendidoResponse> getProductosMasVendidos() {
        log.info("Calculando el Top 5 de productos más vendidos");

        return pedidoDetalleRepository.findTopProductosMasVendidos().stream()
                .limit(5)
                .map(producto -> ProductoMasVendidoResponse.builder()
                        .nombreProducto(producto.getNombreCorto())
                        .marca(producto.getMarca())
                        .cantidadTotalVendida(producto.getCantidadTotal())
                        .build())
                .toList();
    }
}