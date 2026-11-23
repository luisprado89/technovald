package com.technovald.service.impl;

import com.technovald.dto.request.ProveedorRequest;
import com.technovald.dto.response.ProveedorResponse;
import com.technovald.entity.Proveedor;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.ProveedorMapper;
import com.technovald.repository.mysql.ProveedorRepository;
import com.technovald.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Implementación de {@link ProveedorService}. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorMapper proveedorMapper;

    /**
     * Devuelve todos los proveedores (activos e inactivos) ordenados por nombre.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> listarProveedores() {
        return proveedorMapper.toResponseList(proveedorRepository.findAllByOrderByNombreEmpresaAsc());
    }

    /**
     * Devuelve únicamente los proveedores activos, para formularios de pedidos.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> listarProveedoresActivos() {
        return proveedorMapper.toResponseList(proveedorRepository.findAllByActivoTrueOrderByNombreEmpresaAsc());
    }

    /**
     * Crea un nuevo proveedor. Normaliza el CIF a mayúsculas y el email a minúsculas.
     *
     * @throws BusinessRuleException si ya existe un proveedor con ese CIF.
     */
    @Override
    @Transactional
    public ProveedorResponse crearProveedor(ProveedorRequest proveedorRequest) {
        String cif = proveedorRequest.getCif().trim().toUpperCase(Locale.ROOT);

        if (proveedorRepository.existsByCif(cif)) {
            throw new BusinessRuleException("Ya existe un proveedor con ese CIF");
        }

        Proveedor proveedor = Proveedor.builder()
                .nombreEmpresa(proveedorRequest.getNombreEmpresa().trim())
                .cif(cif)
                .contacto(proveedorRequest.getContacto().trim())
                .telefono(proveedorRequest.getTelefono().trim())
                .email(proveedorRequest.getEmail().trim().toLowerCase(Locale.ROOT))
                .direccion(proveedorRequest.getDireccion().trim())
                .activo(true)
                .build();
        proveedorRepository.save(proveedor);

        log.info("Proveedor creado: '{}' (id {}, CIF {})",
                proveedor.getNombreEmpresa(), proveedor.getId(), cif);
        return proveedorMapper.toResponse(proveedor);
    }

    /**
     * Actualiza los datos de un proveedor existente.
     *
     * @throws ResourceNotFoundException si el proveedor no existe.
     * @throws BusinessRuleException     si el nuevo CIF pertenece a otro proveedor.
     */
    @Override
    @Transactional
    public ProveedorResponse actualizarProveedor(Long id, ProveedorRequest proveedorRequest) {
        Proveedor proveedor = proveedorPorId(id);
        String cif = proveedorRequest.getCif().trim().toUpperCase(Locale.ROOT);

        if (proveedorRepository.existsByCifAndIdNot(cif, id)) {
            throw new BusinessRuleException("Ya existe un proveedor con ese CIF");
        }

        proveedor.setNombreEmpresa(proveedorRequest.getNombreEmpresa().trim());
        proveedor.setCif(cif);
        proveedor.setContacto(proveedorRequest.getContacto().trim());
        proveedor.setTelefono(proveedorRequest.getTelefono().trim());
        proveedor.setEmail(proveedorRequest.getEmail().trim().toLowerCase(Locale.ROOT));
        proveedor.setDireccion(proveedorRequest.getDireccion().trim());
        proveedorRepository.save(proveedor);

        log.info("Proveedor actualizado: '{}' (id {})", proveedor.getNombreEmpresa(), id);
        return proveedorMapper.toResponse(proveedor);
    }

    /**
     * Baja lógica: desactiva el proveedor sin eliminarlo de la base de datos.
     */
    @Override
    @Transactional
    public void desactivarProveedor(Long id) {
        Proveedor proveedor = proveedorPorId(id);
        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
        log.info("Proveedor desactivado: id {}", id);
    }

    /**
     * Reactiva un proveedor previamente dado de baja.
     */
    @Override
    @Transactional
    public void reactivarProveedor(Long id) {
        Proveedor proveedor = proveedorPorId(id);
        proveedor.setActivo(true);
        proveedorRepository.save(proveedor);
        log.info("Proveedor reactivado: id {}", id);
    }

    /**
     * Busca un proveedor por ID o lanza una excepción si no existe.
     */
    private Proveedor proveedorPorId(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + id));
    }
}