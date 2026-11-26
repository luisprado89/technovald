package com.technovald.integration;

import com.technovald.document.ProductoDocumento;
import com.technovald.dto.request.ComentarioRequest;
import com.technovald.dto.request.DetallePedidoRequest;
import com.technovald.dto.request.PedidoRequest;
import com.technovald.dto.request.ProductoCatalogoRequest;
import com.technovald.dto.request.RespuestaComentarioRequest;
import com.technovald.dto.response.ComentarioResponse;
import com.technovald.dto.response.PedidoResponse;
import com.technovald.dto.response.ProductoCatalogoDetalleResponse;
import com.technovald.dto.response.ProductoMasVendidoResponse;
import com.technovald.entity.Categoria;
import com.technovald.entity.Cliente;
import com.technovald.entity.Empleado;
import com.technovald.entity.Pedido;
import com.technovald.entity.PedidoDetalle;
import com.technovald.entity.Persona;
import com.technovald.entity.ProductoInventario;
import com.technovald.entity.Proveedor;
import com.technovald.entity.SolicitudReposicion;
import com.technovald.entity.Usuario;
import com.technovald.enums.EstadoSolicitud;
import com.technovald.enums.RolUsuario;
import com.technovald.exception.BusinessRuleException;
import com.technovald.repository.mongo.ProductoMongoRepository;
import com.technovald.repository.mysql.CategoriaRepository;
import com.technovald.repository.mysql.ClienteRepository;
import com.technovald.repository.mysql.EmpleadoRepository;
import com.technovald.repository.mysql.PedidoDetalleRepository;
import com.technovald.repository.mysql.PedidoRepository;
import com.technovald.repository.mysql.PersonaRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import com.technovald.repository.mysql.ProveedorRepository;
import com.technovald.repository.mysql.SolicitudReposicionRepository;
import com.technovald.repository.mysql.UsuarioRepository;
import com.technovald.service.CatalogoService;
import com.technovald.service.ComentarioService;
import com.technovald.service.EstadisticaService;
import com.technovald.service.PedidoService;
import com.technovald.service.SolicitudReposicionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
class TechNovaLdIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("technovald_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:latest");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("technovald_test"));
        registry.add("spring.data.mongodb.uri", () -> MONGO.getReplicaSetUrl("technovald_test"));
        registry.add("app.bootstrap.admin-username", () -> "admin.integration");
        registry.add("app.bootstrap.admin-password", () -> "Admin1234!");
    }

    @Autowired private PedidoService pedidoService;
    @Autowired private SolicitudReposicionService solicitudService;
    @Autowired private EstadisticaService estadisticaService;
    @Autowired private CatalogoService catalogoService;
    @Autowired private ComentarioService comentarioService;

    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private ProductoInventarioRepository productoRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private PedidoDetalleRepository pedidoDetalleRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private SolicitudReposicionRepository solicitudRepository;
    @Autowired private ProductoMongoRepository productoMongoRepository;

    private static final AtomicInteger SEQ = new AtomicInteger(1000);

    private Usuario crearCliente(String etiqueta) {
        int n = SEQ.incrementAndGet();
        Persona persona = Persona.builder()
                .nombre("Test")
                .apellido(etiqueta)
                .dni("T" + String.format("%08d", n))
                .email(etiqueta.toLowerCase() + n + "@test.local")
                .telefono("600000000")
                .direccion("Calle Integración " + n)
                .activo(true)
                .build();
        personaRepository.save(persona);

        Usuario usuario = Usuario.builder()
                .username(etiqueta.toLowerCase() + n)
                .password("hash")
                .rol(RolUsuario.CLIENTE)
                .persona(persona)
                .activo(true)
                .tokenVersion(0)
                .build();
        usuarioRepository.save(usuario);
        persona.setUsuario(usuario);

        clienteRepository.save(Cliente.builder().persona(persona).activo(true).build());
        return usuario;
    }

    private ProductoInventario crearProducto(int stock, BigDecimal precio) {
        int n = SEQ.incrementAndGet();
        Categoria categoria = categoriaRepository.save(Categoria.builder()
                .nombre("Categoría " + n).descripcion("Integración").activo(true).build());
        Proveedor proveedor = proveedorRepository.save(Proveedor.builder()
                .nombreEmpresa("Proveedor " + n)
                .cif("B" + String.format("%08d", n))
                .contacto("Contacto")
                .telefono("986000000")
                .email("proveedor" + n + "@test.local")
                .direccion("Calle Proveedor " + n)
                .activo(true)
                .build());
        return productoRepository.save(ProductoInventario.builder()
                .ean("EAN-" + n)
                .nombreCorto("Producto " + n)
                .stockActual(stock)
                .stockMinimo(1)
                .precioCompra(new BigDecimal("5.00"))
                .precioVenta(precio)
                .marca("Marca")
                .categoria(categoria)
                .proveedor(proveedor)
                .activo(true)
                .build());
    }

    private Empleado crearEmpleado() {
        int n = SEQ.incrementAndGet();
        Persona persona = personaRepository.save(Persona.builder()
                .nombre("Empleado").apellido("Test")
                .dni("E" + String.format("%08d", n))
                .email("empleado" + n + "@test.local")
                .telefono("600000000")
                .direccion("Calle Empleado " + n)
                .activo(true)
                .build());
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .username("empleado" + n)
                .password("hash")
                .rol(RolUsuario.TRABAJADOR)
                .persona(persona)
                .activo(true)
                .tokenVersion(0)
                .build());
        persona.setUsuario(usuario);
        return empleadoRepository.save(Empleado.builder()
                .persona(persona).rol(RolUsuario.TRABAJADOR).activo(true).build());
    }

    private PedidoRequest pedido(ProductoInventario producto, int cantidad) {
        return PedidoRequest.builder()
                .detalles(List.of(DetallePedidoRequest.builder()
                        .productoId(producto.getId())
                        .cantidad(cantidad)
                        .build()))
                .build();
    }

    /** Guarda una ficha mínima en MongoDB para el EAN del producto. */
    private ProductoDocumento crearFichaEnCatalogo(ProductoInventario producto) {
        return productoMongoRepository.save(ProductoDocumento.builder()
                .ean(producto.getEan())
                .titulo("Producto de integración")
                .nombreCorto(producto.getNombreCorto())
                .marca("Marca")
                .categoria("Categoría")
                .activo(true)
                .build());
    }

    private ComentarioRequest comentarioDe(ProductoInventario producto, String texto) {
        return ComentarioRequest.builder()
                .productoInventarioId(producto.getId())
                .texto(texto)
                .valoracion(5)
                .build();
    }

    /** Lanza las tareas a la vez y espera a que terminen todas. */
    private void ejecutarALaVez(List<Runnable> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch listos = new CountDownLatch(tareas.size());
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<?>> futuros = new ArrayList<>();

        for (Runnable tarea : tareas) {
            futuros.add(pool.submit(() -> {
                listos.countDown();
                salida.await(5, TimeUnit.SECONDS);
                tarea.run();
                return null;
            }));
        }

        assertTrue(listos.await(5, TimeUnit.SECONDS));
        salida.countDown();

        for (Future<?> futuro : futuros) {
            futuro.get(15, TimeUnit.SECONDS);
        }
        pool.shutdownNow();
    }

    @Test
    void pedidoDescuentaStockYRechazaLaSegundaCompra() {
        long pedidosAntes = pedidoRepository.count();
        Usuario usuario = crearCliente("atomico");
        ProductoInventario producto = crearProducto(1, new BigDecimal("10.00"));

        PedidoResponse respuesta = pedidoService.crearPedido(pedido(producto, 1), usuario.getUsername());

        assertNotNull(respuesta);
        assertEquals(0, productoRepository.findById(producto.getId()).orElseThrow().getStockActual());
        assertThrows(BusinessRuleException.class,
                () -> pedidoService.crearPedido(pedido(producto, 1), usuario.getUsername()));
        assertEquals(pedidosAntes + 1, pedidoRepository.count());
    }

    @Test
    void dosComprasConcurrentesNoSobrevenden() throws Exception {
        long pedidosAntes = pedidoRepository.count();
        Usuario usuario = crearCliente("concurrente");
        ProductoInventario producto = crearProducto(1, new BigDecimal("10.00"));
        PedidoRequest request = pedido(producto, 1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);
        Callable<Boolean> compra = () -> {
            listos.countDown();
            salida.await(5, TimeUnit.SECONDS);
            try {
                pedidoService.crearPedido(request, usuario.getUsername());
                return true;
            } catch (BusinessRuleException ex) {
                return false;
            }
        };

        Future<Boolean> primera = pool.submit(compra);
        Future<Boolean> segunda = pool.submit(compra);
        assertTrue(listos.await(5, TimeUnit.SECONDS));
        salida.countDown();

        int exitos = (primera.get(15, TimeUnit.SECONDS) ? 1 : 0)
                + (segunda.get(15, TimeUnit.SECONDS) ? 1 : 0);
        pool.shutdownNow();

        assertEquals(1, exitos);
        assertEquals(0, productoRepository.findById(producto.getId()).orElseThrow().getStockActual());
        assertEquals(pedidosAntes + 1, pedidoRepository.count());
    }

    @Test
    void siFallaLaSegundLineaSeRevierteElStockDeLaPrimera() {
        long pedidosAntes = pedidoRepository.count();
        Usuario usuario = crearCliente("rollback");
        ProductoInventario primero = crearProducto(5, new BigDecimal("10.00"));
        ProductoInventario segundo = crearProducto(0, new BigDecimal("20.00"));
        PedidoRequest request = PedidoRequest.builder()
                .detalles(List.of(
                        DetallePedidoRequest.builder().productoId(primero.getId()).cantidad(2).build(),
                        DetallePedidoRequest.builder().productoId(segundo.getId()).cantidad(1).build()))
                .build();

        assertThrows(BusinessRuleException.class,
                () -> pedidoService.crearPedido(request, usuario.getUsername()));

        assertEquals(5, productoRepository.findById(primero.getId()).orElseThrow().getStockActual());
        assertEquals(pedidosAntes, pedidoRepository.count());
    }

    @Test
    void recepcionDeReposicionEsAtomicaYNoSeRepite() {
        Empleado empleado = crearEmpleado();
        ProductoInventario producto = crearProducto(0, new BigDecimal("10.00"));
        Proveedor proveedor = proveedorRepository.findAll().get(0);
        SolicitudReposicion solicitud = solicitudRepository.save(SolicitudReposicion.builder()
                .cantidad(5)
                .producto(producto)
                .proveedor(proveedor)
                .empleado(empleado)
                .estado(EstadoSolicitud.PENDIENTE)
                .activo(true)
                .build());

        solicitudService.recibirSolicitud(solicitud.getId());

        assertEquals(5, productoRepository.findById(producto.getId()).orElseThrow().getStockActual());
        assertThrows(BusinessRuleException.class,
                () -> solicitudService.recibirSolicitud(solicitud.getId()));
        assertEquals(5, productoRepository.findById(producto.getId()).orElseThrow().getStockActual());
        assertEquals(EstadoSolicitud.RECIBIDO,
                solicitudRepository.findById(solicitud.getId()).orElseThrow().getEstado());
    }

    @Test
    void elTopCincoSeCalculaConDatosReales() {
        Usuario usuario = crearCliente("top");
        Cliente cliente = clienteRepository.findByPersonaIdAndActivoTrue(usuario.getPersona().getId())
                .orElseThrow();

        for (int i = 0; i < 6; i++) {
            ProductoInventario producto = crearProducto(100, new BigDecimal("10.00"));
            Pedido pedido = pedidoRepository.save(Pedido.builder()
                    .cliente(cliente)
                    .total(new BigDecimal("10.00").multiply(BigDecimal.valueOf(10 - i)))
                    .build());
            pedidoDetalleRepository.save(PedidoDetalle.builder()
                    .pedido(pedido)
                    .producto(producto)
                    .cantidad(10 - i)
                    .precioUnitario(new BigDecimal("10.00"))
                    .subtotal(new BigDecimal("10.00").multiply(BigDecimal.valueOf(10 - i)))
                    .build());
        }

        List<ProductoMasVendidoResponse> top = estadisticaService.getProductosMasVendidos();

        assertEquals(5, top.size());
        assertEquals(10L, top.get(0).getCantidadTotalVendida());
        assertEquals(6L, top.get(4).getCantidadTotalVendida());
    }

    @Test
    void elCatalogoResuelvePrecioYStockPorEan() {
        ProductoInventario producto = crearProducto(7, new BigDecimal("99.99"));
        productoMongoRepository.save(ProductoDocumento.builder()
                .ean(producto.getEan())
                .titulo("Producto de integración")
                .nombreCorto(producto.getNombreCorto())
                .marca("Marca")
                .categoria("Categoría")
                .activo(true)
                .build());

        ProductoCatalogoDetalleResponse detalle =
                catalogoService.obtenerProductoCatalogoPorEan(producto.getEan());

        assertEquals(producto.getEan(), detalle.getEan());
        assertEquals(7, detalle.getStockActual());
        assertEquals(0, new BigDecimal("99.99").compareTo(detalle.getPrecioVenta()));
    }

    @Test
    void laMigracionFlywayMantieneElDniUnico() {
        int n = SEQ.incrementAndGet();
        String dni = "U" + String.format("%08d", n);
        personaRepository.saveAndFlush(Persona.builder()
                .nombre("Uno").apellido("Test").dni(dni)
                .email("uno" + n + "@test.local").telefono("600000000")
                .direccion("Calle Uno").activo(true).build());

        Persona duplicada = Persona.builder()
                .nombre("Dos").apellido("Test").dni(dni)
                .email("dos" + n + "@test.local").telefono("600000001")
                .direccion("Calle Dos").activo(true).build();

        assertThrows(DataIntegrityViolationException.class,
                () -> personaRepository.saveAndFlush(duplicada));
    }

    @Test
    void dosComentariosConcurrentesNoSePierden() throws Exception {
        Usuario cliente = crearCliente("comentaconcurrente");
        ProductoInventario producto = crearProducto(10, new BigDecimal("10.00"));
        crearFichaEnCatalogo(producto);

        ComentarioRequest request = comentarioDe(producto, "Comentario concurrente");

        ejecutarALaVez(List.of(
                () -> comentarioService.crearComentario(request, cliente.getUsername()),
                () -> comentarioService.crearComentario(request, cliente.getUsername())));

        ProductoDocumento ficha = productoMongoRepository.findByEan(producto.getEan()).orElseThrow();
        assertEquals(2, ficha.getComentarios().size());
    }

    @Test
    void dosRespuestasConcurrentesSeConservan() throws Exception {
        Usuario cliente = crearCliente("respuestaconcurrente");
        String staff = crearEmpleado().getPersona().getUsuario().getUsername();
        ProductoInventario producto = crearProducto(10, new BigDecimal("10.00"));
        crearFichaEnCatalogo(producto);

        ComentarioResponse primero = comentarioService.crearComentario(
                comentarioDe(producto, "Primera"), cliente.getUsername());
        ComentarioResponse segundo = comentarioService.crearComentario(
                comentarioDe(producto, "Segunda"), cliente.getUsername());

        ejecutarALaVez(List.of(
                () -> comentarioService.responderComentario(primero.getId(),
                        new RespuestaComentarioRequest("Respuesta A"), staff),
                () -> comentarioService.responderComentario(segundo.getId(),
                        new RespuestaComentarioRequest("Respuesta B"), staff)));

        ProductoDocumento ficha = productoMongoRepository.findByEan(producto.getEan()).orElseThrow();
        assertTrue(ficha.getComentarios().stream()
                .anyMatch(c -> primero.getId().equals(c.getId()) && c.getRespuestaOficial() != null));
        assertTrue(ficha.getComentarios().stream()
                .anyMatch(c -> segundo.getId().equals(c.getId()) && c.getRespuestaOficial() != null));
    }

    @Test
    void guardarLaFichaMientrasSeComentaNoPierdeElComentario() throws Exception {
        Usuario cliente = crearCliente("catalogoconcurrente");
        ProductoInventario producto = crearProducto(10, new BigDecimal("10.00"));
        crearFichaEnCatalogo(producto);

        ProductoCatalogoRequest request = ProductoCatalogoRequest.builder()
                .ean(producto.getEan())
                .titulo("Título actualizado")
                .build();

        ejecutarALaVez(List.of(
                () -> catalogoService.guardarProductoCatalogo(request),
                () -> comentarioService.crearComentario(
                        comentarioDe(producto, "Sobrevive al guardado"), cliente.getUsername())));

        ProductoDocumento ficha = productoMongoRepository.findByEan(producto.getEan()).orElseThrow();
        assertEquals(1, ficha.getComentarios().size());
        assertEquals("Título actualizado", ficha.getTitulo());
    }
}
