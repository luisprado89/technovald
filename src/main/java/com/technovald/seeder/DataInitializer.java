package com.technovald.seeder;

import com.technovald.client.IcecatClient;
import com.technovald.document.ProductoDocumento;
import com.technovald.dto.icecat.IcecatResponse;
import com.technovald.dto.request.ComentarioRequest;
import com.technovald.dto.request.DetallePedidoRequest;
import com.technovald.dto.request.PedidoRequest;
import com.technovald.dto.request.RespuestaComentarioRequest;
import com.technovald.dto.request.SolicitudReposicionRequest;
import com.technovald.dto.response.ComentarioResponse;
import com.technovald.dto.response.PedidoResponse;
import com.technovald.dto.response.SolicitudReposicionResponse;
import com.technovald.entity.Categoria;
import com.technovald.entity.Cliente;
import com.technovald.entity.Empleado;
import com.technovald.entity.Persona;
import com.technovald.entity.ProductoInventario;
import com.technovald.entity.Proveedor;
import com.technovald.entity.Usuario;
import com.technovald.enums.EstadoPedido;
import com.technovald.enums.RolUsuario;
import com.technovald.mapper.IcecatMapper;
import com.technovald.repository.mongo.ProductoMongoRepository;
import com.technovald.repository.mysql.CategoriaRepository;
import com.technovald.repository.mysql.ClienteRepository;
import com.technovald.repository.mysql.EmpleadoRepository;
import com.technovald.repository.mysql.PersonaRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import com.technovald.repository.mysql.ProveedorRepository;
import com.technovald.repository.mysql.UsuarioRepository;
import com.technovald.service.ComentarioService;
import com.technovald.service.PedidoService;
import com.technovald.service.SolicitudReposicionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga los datos de demostración de TechNovaLD en el perfil de desarrollo.
 *
 * <p>El seeder es deliberadamente independiente de
 * {@link AdminBootstrapInitializer}: el administrador inicial se crea siempre
 * que falte, mientras que estos datos solo existen en {@code dev} y solo si
 * {@code app.seed.enabled=true}. Es idempotente: si ya hay categorías o
 * clientes, no vuelve a insertar nada.</p>
 *
 * <p><b>Icecat:</b> antes de guardar cada producto se consulta su EAN. Si la
 * cuenta gratuita no devuelve la ficha (403/404), el producto se omite y se
 * registra el motivo. Solo se crea inventario y documento MongoDB para las
 * fichas que Icecat confirma. La lista de EAN está validada contra Open
 * Icecat; no se incluyen marcas que solo están en Full Icecat.</p>
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Demo1234!";
    private static final String DNI_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoInventarioRepository productoRepository;
    private final ProductoMongoRepository productoMongoRepository;
    private final PasswordEncoder passwordEncoder;
    private final IcecatClient icecatClient;
    private final IcecatMapper icecatMapper;
    private final PedidoService pedidoService;
    private final ComentarioService comentarioService;
    private final SolicitudReposicionService solicitudReposicionService;

    private String demoPasswordHash;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoriaRepository.count() > 0 || clienteRepository.count() > 0) {
            log.info("Seed de demostración omitido: ya existen categorías o clientes.");
            return;
        }

        log.info("=== SEED DE DEMOSTRACIÓN TECHNOVALD ===");
        demoPasswordHash = passwordEncoder.encode(DEMO_PASSWORD);

        Map<String, Categoria> categorias = seedCategorias();
        Map<String, Proveedor> proveedores = seedProveedores();
        Map<String, Cliente> clientes = seedClientes();
        Map<String, Empleado> empleados = seedEmpleados();
        List<ProductoInventario> productos = seedProductos(categorias, proveedores);

        int comentarios = seedComentarios(productos);
        int pedidos = seedPedidos(productos);
        int reposiciones = seedReposiciones(productos);

        log.info("=== SEED DE DEMOSTRACIÓN COMPLETADO ===");
        log.info("Categorías: {} | Proveedores: {} | Clientes: {} | Trabajadores: {}",
                categorias.size(), proveedores.size(), clientes.size(), empleados.size());
        log.info("Productos validados y sincronizados con Icecat: {}", productos.size());
        log.info("Reseñas: {} | Pedidos: {} | Reposiciones: {}", comentarios, pedidos, reposiciones);
        log.info("Usuarios demo: cliente01..cliente20 / trabajador01..trabajador05");
        log.info("Contraseña común de demo: {}", DEMO_PASSWORD);
    }

    // ------------------------------------------------------------------
    // Maestros
    // ------------------------------------------------------------------

    private Map<String, Categoria> seedCategorias() {
        Map<String, Categoria> resultado = new LinkedHashMap<>();
        for (CategoriaSeed seed : List.of(
                new CategoriaSeed("Portátiles", "Equipos portátiles para uso profesional y doméstico."),
                new CategoriaSeed("Periféricos", "Ratones, teclados, webcams y auriculares."),
                new CategoriaSeed("Monitores", "Pantallas de 24 a 32 pulgadas."),
                new CategoriaSeed("Almacenamiento", "SSD, discos duros y soluciones NVMe."),
                new CategoriaSeed("Redes", "Routers, switches y conectividad."),
                new CategoriaSeed("Impresión", "Impresoras multifunción y consumibles.")
        )) {
            Categoria categoria = categoriaRepository.save(Categoria.builder()
                    .nombre(seed.nombre())
                    .descripcion(seed.descripcion())
                    .activo(true)
                    .build());
            resultado.put(seed.nombre(), categoria);
        }
        return resultado;
    }

    private Map<String, Proveedor> seedProveedores() {
        Map<String, Proveedor> resultado = new LinkedHashMap<>();
        for (ProveedorSeed seed : List.of(
                new ProveedorSeed("Dell Iberia", "B10000001", "Laura Méndez", "600100001",
                        "compras@dell-iberia.demo", "Parque Empresarial, Vigo"),
                new ProveedorSeed("Logitech España", "B10000002", "Carlos Núñez", "600100002",
                        "compras@logitech-espana.demo", "Calle Tecnología 2, Vigo"),
                new ProveedorSeed("Samsung Iberia", "B10000003", "Marta Otero", "600100003",
                        "compras@samsung-iberia.demo", "Avenida Digital 3, Vigo"),
                new ProveedorSeed("ASUS Iberia", "B10000004", "Javier Lamas", "600100004",
                        "compras@asus-iberia.demo", "Rúa das Industrias 4, Vigo"),
                new ProveedorSeed("Acer Iberia", "B10000005", "Nuria Paz", "600100005",
                        "compras@acer-iberia.demo", "Polígono A Granxa 5, Vigo"),
                new ProveedorSeed("Philips Ibérica", "B10000006", "Diego Varela", "600100006",
                        "compras@philips-iberica.demo", "Calle Luz 6, Vigo"),
                new ProveedorSeed("Kingston Technology", "B10000007", "Sara Freire", "600100007",
                        "compras@kingston-iberia.demo", "Rúa Memoria 7, Vigo"),
                new ProveedorSeed("Seagate Iberia", "B10000008", "Pablo Souto", "600100008",
                        "compras@seagate-iberia.demo", "Avenida Datos 8, Vigo"),
                new ProveedorSeed("TP-Link Iberia", "B10000009", "Elena Barros", "600100009",
                        "compras@tp-link-iberia.demo", "Calle Red 9, Vigo"),
                new ProveedorSeed("Distribuidor Impresión Ibérica", "B10000010", "Hugo Piñeiro", "600100010",
                        "compras@impresion-iberica.demo", "Rúa Tinta 10, Vigo")
        )) {
            Proveedor proveedor = proveedorRepository.save(Proveedor.builder()
                    .nombreEmpresa(seed.nombreEmpresa())
                    .cif(seed.cif())
                    .contacto(seed.contacto())
                    .telefono(seed.telefono())
                    .email(seed.email())
                    .direccion(seed.direccion())
                    .activo(true)
                    .build());
            resultado.put(seed.nombreEmpresa(), proveedor);
        }
        return resultado;
    }

    // ------------------------------------------------------------------
    // Identidad: 20 clientes y 5 trabajadores
    // ------------------------------------------------------------------

    private Map<String, Cliente> seedClientes() {
        Map<String, Cliente> resultado = new LinkedHashMap<>();
        List<ClienteSeed> seeds = List.of(
                new ClienteSeed("Lucía", "García"), new ClienteSeed("Mateo", "Fernández"),
                new ClienteSeed("Carmen", "Rodríguez"), new ClienteSeed("Javier", "López"),
                new ClienteSeed("Laura", "Martínez"), new ClienteSeed("Pablo", "Sánchez"),
                new ClienteSeed("Ana", "Pérez"), new ClienteSeed("Sergio", "Gómez"),
                new ClienteSeed("Marta", "Díaz"), new ClienteSeed("Álvaro", "Torres"),
                new ClienteSeed("Elena", "Ruiz"), new ClienteSeed("David", "Romero"),
                new ClienteSeed("Paula", "Navarro"), new ClienteSeed("Hugo", "Domínguez"),
                new ClienteSeed("Sara", "Vázquez"), new ClienteSeed("Iván", "Ramos"),
                new ClienteSeed("Noelia", "Gil"), new ClienteSeed("Adrián", "Serrano"),
                new ClienteSeed("Clara", "Molina"), new ClienteSeed("Rubén", "Ortega")
        );

        for (int i = 0; i < seeds.size(); i++) {
            int numero = i + 1;
            ClienteSeed seed = seeds.get(i);
            String username = String.format("cliente%02d", numero);
            Persona persona = personaRepository.save(Persona.builder()
                    .nombre(seed.nombre())
                    .apellido(seed.apellido())
                    .dni(dni(10_000_000 + numero))
                    .email(username + "@yopmail.com")
                    .telefono(String.format("600%06d", 100_000 + numero))
                    .direccion("Calle Demo " + numero + ", Vigo")
                    .activo(true)
                    .build());

            Usuario usuario = usuarioRepository.save(Usuario.builder()
                    .username(username)
                    .password(demoPasswordHash)
                    .rol(RolUsuario.CLIENTE)
                    .persona(persona)
                    .activo(true)
                    .tokenVersion(0)
                    .build());
            persona.setUsuario(usuario);
            personaRepository.save(persona);

            Cliente cliente = clienteRepository.save(Cliente.builder()
                    .persona(persona)
                    .activo(true)
                    .build());
            persona.setCliente(cliente);
            personaRepository.save(persona);
            resultado.put(username, cliente);
        }
        return resultado;
    }

    private Map<String, Empleado> seedEmpleados() {
        Map<String, Empleado> resultado = new LinkedHashMap<>();
        List<ClienteSeed> seeds = List.of(
                new ClienteSeed("Elena", "Vidal"), new ClienteSeed("Marcos", "Castro"),
                new ClienteSeed("Nuria", "Ferreiro"), new ClienteSeed("Iago", "Souto"),
                new ClienteSeed("Alba", "Pereira")
        );

        for (int i = 0; i < seeds.size(); i++) {
            int numero = i + 1;
            ClienteSeed seed = seeds.get(i);
            String username = String.format("trabajador%02d", numero);
            Persona persona = personaRepository.save(Persona.builder()
                    .nombre(seed.nombre())
                    .apellido(seed.apellido())
                    .dni(dni(20_000_000 + numero))
                    .email(username + "@yopmail.com")
                    .telefono(String.format("600%06d", 200_000 + numero))
                    .direccion("Calle Taller " + numero + ", Vigo")
                    .activo(true)
                    .build());

            Usuario usuario = usuarioRepository.save(Usuario.builder()
                    .username(username)
                    .password(demoPasswordHash)
                    .rol(RolUsuario.TRABAJADOR)
                    .persona(persona)
                    .activo(true)
                    .tokenVersion(0)
                    .build());
            persona.setUsuario(usuario);
            personaRepository.save(persona);

            Empleado empleado = empleadoRepository.save(Empleado.builder()
                    .persona(persona)
                    .rol(RolUsuario.TRABAJADOR)
                    .activo(true)
                    .build());
            persona.setEmpleado(empleado);
            personaRepository.save(persona);
            resultado.put(username, empleado);
        }
        return resultado;
    }

    // ------------------------------------------------------------------
    // Catálogo validado con Icecat
    // ------------------------------------------------------------------

    private List<ProductoInventario> seedProductos(Map<String, Categoria> categorias,
                                                   Map<String, Proveedor> proveedores) {
        List<ProductoInventario> resultado = new ArrayList<>();
        for (ProductoSeed seed : productosSeed()) {
            IcecatResponse respuesta;
            try {
                respuesta = icecatClient.obtenerProducto(seed.ean());
            } catch (RuntimeException ex) {
                log.warn("Producto omitido: Icecat no devuelve el EAN {} ({}).", seed.ean(), ex.getMessage());
                continue;
            }

            if (respuesta.getData() == null || respuesta.getData().getGeneralInfo() == null) {
                log.warn("Producto omitido: Icecat devuelve el EAN {} sin GeneralInfo.", seed.ean());
                continue;
            }

            BigDecimal precioVenta = new BigDecimal(seed.precio()).setScale(2, RoundingMode.HALF_UP);
            ProductoInventario inventario = productoRepository.save(ProductoInventario.builder()
                    .ean(seed.ean())
                    .nombreCorto(seed.nombreCorto())
                    .marca(seed.marca())
                    .stockActual(seed.stock())
                    .stockMinimo(3)
                    .precioCompra(precioVenta.multiply(new BigDecimal("0.72")).setScale(2, RoundingMode.HALF_UP))
                    .precioVenta(precioVenta)
                    .categoria(categorias.get(seed.categoria()))
                    .proveedor(proveedores.get(seed.proveedor()))
                    .activo(true)
                    .build());

            try {
                ProductoDocumento documento = icecatMapper.toDocumento(respuesta, seed.ean(), null);
                documento.setFechaSincronizacion(LocalDateTime.now());
                productoMongoRepository.save(documento);
            } catch (RuntimeException ex) {
                productoRepository.delete(inventario);
                log.warn("Producto omitido: no se pudo crear la ficha MongoDB del EAN {} ({}).",
                        seed.ean(), ex.getMessage());
                continue;
            }

            resultado.add(inventario);
            log.info("Producto validado y sincronizado: {} ({})", seed.nombreCorto(), seed.ean());
        }
        return resultado;
    }

    private List<ProductoSeed> productosSeed() {
        return List.of(
                new ProductoSeed("5397184119709", "Dell G5 5587", "Dell", "Portátiles", "Dell Iberia", "899.00", 8),
                new ProductoSeed("5099206064201", "Logitech C270 HD", "Logitech", "Periféricos", "Logitech España", "39.90", 25),
                new ProductoSeed("0097855074126", "Logitech M185", "Logitech", "Periféricos", "Logitech España", "14.90", 40),
                new ProductoSeed("0097855065537", "Logitech K120", "Logitech", "Periféricos", "Logitech España", "17.90", 35),
                new ProductoSeed("5099206066670", "Logitech M330 Silent Plus", "Logitech", "Periféricos", "Logitech España", "24.90", 30),
                new ProductoSeed("0097855089816", "Logitech MK270", "Logitech", "Periféricos", "Logitech España", "39.90", 20),
                new ProductoSeed("5099206038844", "Logitech H340 USB", "Logitech", "Periféricos", "Logitech España", "29.90", 22),
                new ProductoSeed("8806092005686", "Samsung C27G55TQWR", "Samsung", "Monitores", "Samsung Iberia", "279.00", 12),
                new ProductoSeed("4718017451284", "ASUS VA24EHE", "ASUS", "Monitores", "ASUS Iberia", "129.00", 18),
                new ProductoSeed("4713883647656", "Acer Nitro VG240Y", "Acer", "Monitores", "Acer Iberia", "139.00", 15),
                new ProductoSeed("8712581742331", "Philips 243V7QDSB", "Philips", "Monitores", "Philips Ibérica", "119.00", 20),
                new ProductoSeed("0887276437224", "Samsung 980 1 TB", "Samsung", "Almacenamiento", "Samsung Iberia", "89.90", 30),
                new ProductoSeed("0740617263442", "Kingston A400 480 GB", "Kingston", "Almacenamiento", "Kingston Technology", "39.90", 40),
                new ProductoSeed("0763649100165", "Seagate Barracuda 2 TB", "Seagate", "Almacenamiento", "Seagate Iberia", "59.90", 25),
                new ProductoSeed("4897098682678", "TP-Link Archer AX55", "TP-Link", "Redes", "TP-Link Iberia", "99.90", 15),
                new ProductoSeed("0172304263228", "TP-Link TL-SG105", "TP-Link", "Redes", "TP-Link Iberia", "24.90", 30),
                new ProductoSeed("8715946686349", "Epson EcoTank ET-2856", "Epson", "Impresión", "Distribuidor Impresión Ibérica", "299.00", 10),
                new ProductoSeed("4549292143867", "Canon PIXMA TS3350", "Canon", "Impresión", "Distribuidor Impresión Ibérica", "59.90", 18)
        );
    }

    // ------------------------------------------------------------------
    // Reseñas
    // ------------------------------------------------------------------

    private int seedComentarios(List<ProductoInventario> productos) {
        Map<String, ProductoInventario> porNombre = indexar(productos);
        List<ComentarioSeed> seeds = List.of(
                new ComentarioSeed("Dell G5 5587", "cliente01", "Equipo muy rápido para desarrollo y con buena refrigeración.", 5),
                new ComentarioSeed("Dell G5 5587", "cliente02", "Cumple para trabajo diario; el teclado podría ser mejor.", 4),
                new ComentarioSeed("Samsung C27G55TQWR", "cliente03", "Monitor muy nítido y el soporte es estable.", 5),
                new ComentarioSeed("Samsung C27G55TQWR", "cliente04", "Buena relación calidad-precio para 1440p.", 4),
                new ComentarioSeed("Logitech C270 HD", "cliente05", "Perfecta para videollamadas, imagen clara.", 5),
                new ComentarioSeed("Logitech C270 HD", "cliente06", "Sencilla de configurar y funciona bien.", 4),
                new ComentarioSeed("Samsung 980 1 TB", "cliente07", "Arranque y carga de proyectos muy rápidos.", 5),
                new ComentarioSeed("Samsung 980 1 TB", "cliente08", "Buena SSD para el precio; instala sin problemas.", 5),
                new ComentarioSeed("TP-Link Archer AX55", "cliente09", "Cobertura estable en toda la oficina.", 5),
                new ComentarioSeed("TP-Link Archer AX55", "cliente10", "Fácil de configurar y buena velocidad.", 4)
        );

        int creados = 0;
        for (int i = 0; i < seeds.size(); i++) {
            ComentarioSeed seed = seeds.get(i);
            ProductoInventario producto = porNombre.get(seed.producto());
            if (producto == null) {
                continue;
            }
            ComentarioResponse comentario = comentarioService.crearComentario(
                    ComentarioRequest.builder()
                            .productoInventarioId(producto.getId())
                            .texto(seed.texto())
                            .valoracion(seed.valoracion())
                            .build(),
                    seed.username());
            creados++;

            if (i % 2 == 0) {
                comentarioService.responderComentario(
                        comentario.getId(),
                        new RespuestaComentarioRequest("Gracias por tu valoración. Seguimos a tu disposición."),
                        "trabajador01");
            }
        }
        return creados;
    }

    // ------------------------------------------------------------------
    // Pedidos
    // ------------------------------------------------------------------

    private int seedPedidos(List<ProductoInventario> productos) {
        Map<String, ProductoInventario> porNombre = indexar(productos);

        PedidoResponse pedido1 = crearPedido("cliente01", porNombre.get("Dell G5 5587"), 1);
        PedidoResponse pedido2 = crearPedido("cliente02", porNombre.get("Samsung C27G55TQWR"), 1);
        PedidoResponse pedido3 = crearPedido("cliente03", porNombre.get("Logitech MK270"), 2);
        PedidoResponse pedido4 = crearPedido("cliente04", porNombre.get("Samsung 980 1 TB"), 1);
        PedidoResponse pedido5 = crearPedido("cliente05", porNombre.get("TP-Link Archer AX55"), 1);

        if (pedido2 != null) {
            pedidoService.updateEstadoPedido(pedido2.getId(), EstadoPedido.PROCESANDO);
        }
        if (pedido3 != null) {
            pedidoService.updateEstadoPedido(pedido3.getId(), EstadoPedido.PROCESANDO);
            pedidoService.updateEstadoPedido(pedido3.getId(), EstadoPedido.ENVIADO);
        }
        if (pedido4 != null) {
            pedidoService.updateEstadoPedido(pedido4.getId(), EstadoPedido.PROCESANDO);
            pedidoService.updateEstadoPedido(pedido4.getId(), EstadoPedido.ENVIADO);
            pedidoService.updateEstadoPedido(pedido4.getId(), EstadoPedido.ENTREGADO);
        }

        int creados = 0;
        for (PedidoResponse pedido : new PedidoResponse[]{pedido1, pedido2, pedido3, pedido4, pedido5}) {
            if (pedido != null) {
                creados++;
            }
        }
        return creados;
    }

    private PedidoResponse crearPedido(String username, ProductoInventario producto, int cantidad) {
        if (producto == null) {
            return null;
        }
        return pedidoService.crearPedido(
                PedidoRequest.builder()
                        .detalles(List.of(DetallePedidoRequest.builder()
                                .productoId(producto.getId())
                                .cantidad(cantidad)
                                .build()))
                        .build(),
                username);
    }

    // ------------------------------------------------------------------
    // Reposiciones
    // ------------------------------------------------------------------

    private int seedReposiciones(List<ProductoInventario> productos) {
        Map<String, ProductoInventario> porNombre = indexar(productos);
        List<SolicitudReposicionResponse> solicitudes = new ArrayList<>();

        solicitudes.add(crearReposicion("trabajador01", porNombre.get("TP-Link Archer AX55"), 10));
        solicitudes.add(crearReposicion("trabajador02", porNombre.get("Epson EcoTank ET-2856"), 5));
        solicitudes.add(crearReposicion("trabajador03", porNombre.get("Dell G5 5587"), 2));

        if (!solicitudes.isEmpty() && solicitudes.get(0) != null) {
            solicitudReposicionService.recibirSolicitud(solicitudes.get(0).getId());
        }

        return (int) solicitudes.stream().filter(s -> s != null).count();
    }

    private SolicitudReposicionResponse crearReposicion(String username,
                                                        ProductoInventario producto,
                                                        int cantidad) {
        if (producto == null) {
            return null;
        }
        return solicitudReposicionService.crearSolicitud(
                SolicitudReposicionRequest.builder()
                        .productoId(producto.getId())
                        .proveedorId(producto.getProveedor().getId())
                        .cantidad(cantidad)
                        .build(),
                username);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Map<String, ProductoInventario> indexar(List<ProductoInventario> productos) {
        Map<String, ProductoInventario> resultado = new LinkedHashMap<>();
        for (ProductoInventario producto : productos) {
            resultado.put(producto.getNombreCorto(), producto);
        }
        return resultado;
    }

    private String dni(int numero) {
        return String.format("%08d%c", numero, DNI_LETTERS.charAt(numero % 23));
    }

    private record CategoriaSeed(String nombre, String descripcion) {
    }

    private record ProveedorSeed(String nombreEmpresa, String cif, String contacto,
                                 String telefono, String email, String direccion) {
    }

    private record ClienteSeed(String nombre, String apellido) {
    }

    private record ProductoSeed(String ean, String nombreCorto, String marca, String categoria,
                                String proveedor, String precio, int stock) {
    }

    private record ComentarioSeed(String producto, String username, String texto, int valoracion) {
    }
}
