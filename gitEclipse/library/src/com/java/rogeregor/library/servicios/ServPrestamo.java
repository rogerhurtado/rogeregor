package com.java.rogeregor.library.servicios;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Map;
import java.util.Scanner;

import com.java.rogeregor.library.modelo.articulos.*;
import com.java.rogeregor.library.modelo.prestamos.Prestamo;
import com.java.rogeregor.library.modelo.prestamos.TicketPrestamo;
import com.java.rogeregor.library.modelo.usuarios.*;
import com.java.rogeregor.library.usuarioDAO.UsuarioDAO;
import com.java.rogeregor.library.usuarioDAO.UsuarioDAOImpl;

import interfaces.DeudaPrestamo;
import com.java.rogeregor.library.Principal;
import com.java.rogeregor.library.articuloDAO.ArticuloDAO;
import com.java.rogeregor.library.articuloDAO.ArticuloDAOImpl;
import com.java.rogeregor.library.util.*;

public class ServPrestamo<U extends Persona, T extends Ejemplar> implements DeudaPrestamo {

    private Scanner sc = new Scanner(System.in);
    
    private final UsuarioDAO<U> user;
    private final ArticuloDAO<T> arti;

    public ServPrestamo(UsuarioDAO<U> user, ArticuloDAO<T> arti) {
        this.user = user;
        this.arti = arti;
    }

	@Override
	public boolean test(LocalDate fechaFin) {
		long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), fechaFin);
        return diasRestantes < 0; // Hay deuda si la fecha de fin ya pasó
	}
	
	
	public void agregarDeudores(){
	    U cliente = user.createUsuario((Class<U>) Cliente.class, Valor.getDeudorId());

	    if (cliente == null || !(cliente instanceof Cliente)) {
	        System.out.println("No es cliente");
	        return; // Salir del método si el cliente es null
	    }

	    if (Principal.deudores.containsKey(cliente)) {
	        System.out.println("El cliente está en la lista de Morosos.");
	    } else {
            String dni = solicitarTexto("DNI del Cliente");
	        Principal.deudores.put(dni, (Cliente) cliente);
	        System.out.println("Cliente agregado a la lista de deudores.");
	    }
	    
	    System.out.println("Lista de deudores: " + Principal.deudores);
	}

    public void devolverPrestamo() {
        System.out.println("--------devolución de artículo----------------");
        int clienteId = solicitarIdCliente();
        Cliente cliente = (Cliente) user.seleccionarUsuario((Class<U>) Cliente.class, clienteId);


        if (cliente != null && Principal.usuarios.contains(cliente)) {
            String devolver;
            do {
                if (devolucion(cliente) == EstadoPrestamo.DEVUELTO) {
                    System.out.println("¿Desea devolver otro préstamo? (si/no)");
                    devolver = sc.next();
                    sc.nextLine(); // Limpiar buffer
                    System.out.println("Préstamo devuelto");
                } else {
                    System.out.println("No se pudo realizar la devolución.");
                    break;
                }
            } while (devolver.equalsIgnoreCase("si"));
        } else {
            System.out.println("Usuario no encontrado.");
        }
    }

    private EstadoPrestamo devolucion(Cliente cliente) {
        System.out.println("¿Qué tipo de artículo desea devolver? (1-Libro, 2-Revista)");
        int tipoArticulo = sc.nextInt();
        sc.nextLine();  // Limpiar buffer

        Class<? extends Ejemplar> clazz = tipoArticulo == 1 ? Libro.class : Revista.class;
        if (clazz == null) {
            System.out.println("Tipo de artículo no válido.");
            return EstadoPrestamo.ANY_STATE;
        }

        System.out.println("Introduce el código del artículo: ");
        int codSolicitado = sc.nextInt();
        sc.nextLine();
        Ejemplar ejemplar = seleccionarArticulo(clazz, codSolicitado);
        
        if (ejemplar == null) {
            System.out.println("El ejemplar no se encuentra.");
            return EstadoPrestamo.ANY_STATE;
        }

        if (ejemplar.getEstado() == EstadoPrestamo.EN_CURSO) {
            ejemplar.setEstado(EstadoPrestamo.DEVUELTO);
            aumentarStock(ejemplar);
           
         // Actualizar la lista de préstamos
            for (int i = 0; i < Principal.prestamos.size(); i++) {
                Prestamo prestamo = Principal.prestamos.get(i);
                if (prestamo.getEjemplar().equals(ejemplar) && prestamo.getCliente().equals(cliente)) {
                	 int stockActual = Principal.inventario.getOrDefault(ejemplar, 0);
                    Principal.prestamos.set(i, new Prestamo(prestamo.getCliente(), prestamo.getEjemplar(), prestamo.getFechaInicio(), LocalDate.now(), EstadoPrestamo.DEVUELTO, stockActual));
                    break;
                }
            }
            System.out.println("Artículo devuelto correctamente.");
            return EstadoPrestamo.DEVUELTO;
        } else {
            System.out.println("El artículo no estaba prestado.");
            return ejemplar.getEstado();
        }
    }

    public void pedirPrestamo() {
        System.out.println("-------- Petición de Artículo --------");

        int clienteId = solicitarIdCliente();
        Cliente cliente = (Cliente) user.seleccionarUsuario((Class<U>) Cliente.class, clienteId);

        if (cliente != null) {
            String agregar;
            do {
                if (peticion(cliente) == EstadoPrestamo.EN_CURSO) {
                    System.out.println("¿Desea añadir otro préstamo? (si/no)");
                    agregar = sc.next();
                    sc.nextLine();
                    aumentarContadorPrestamos(cliente);
                    System.out.println("Préstamo añadido");
                    System.out.println("---------- Ticket del Préstamo ----------");
                    imprimirTicket(cliente);
                } else {
                    System.out.println("No se pudo realizar el préstamo.");
                    break;
                }
            } while (agregar.equalsIgnoreCase("si"));

        } else {
            System.out.println("Usuario no encontrado.");
        }
    }

    private EstadoPrestamo peticion(Cliente cliente) {
        // Preguntar al usuario qué tipo de artículo quiere (Libro o Revista)
        System.out.println("¿Qué tipo de artículo desea solicitar? (1-Libro, 2-Revista)");
        int tipoArticulo = sc.nextInt();
        sc.nextLine();  // Limpiar el buffer del Scanner

        Class<? extends Ejemplar> clazz;
        if (tipoArticulo == 1) {
            clazz = Libro.class;
        } else if (tipoArticulo == 2) {
            clazz = Revista.class;
        } else {
            System.out.println("Tipo de artículo no válido.");
            return EstadoPrestamo.ANY_STATE;
        }

        System.out.println("Introduce el código del artículo: ");
        int codSolicitado = sc.nextInt();
        sc.nextLine();  // Limpiar el buffer

        Ejemplar ejemplarSolicitado = seleccionarArticulo(clazz, codSolicitado);
        
        if (ejemplarSolicitado != null) {
            EstadoPrestamo estado = ejemplarSolicitado.getEstado();
         // Reducir el stock del ejemplar
            reducirStock(ejemplarSolicitado);
            int stockActual = Principal.inventario.getOrDefault(ejemplarSolicitado, 0);

            if (stockActual >= 1 && estado == EstadoPrestamo.DEVUELTO) {
                ejemplarSolicitado.setEstado(EstadoPrestamo.EN_CURSO);
                LocalDate fechaInicio = LocalDate.now();
                LocalDate fechaFin = fechaInicio.plusDays(15); // Ajustar el período del préstamo según sea necesario

                // Crear un nuevo objeto Prestamo usando el constructor adecuado
                Prestamo nuevoPrestamo = new Prestamo(cliente, ejemplarSolicitado, fechaInicio, fechaFin, EstadoPrestamo.EN_CURSO, stockActual);
             

                // Agregar el nuevo préstamo a la lista de préstamos
                Principal.prestamos.add(nuevoPrestamo);
                //Agregar el nuevo prestamo al mapa de enCurso
                Principal.enCurso.put(nuevoPrestamo.getPrestamoId(), nuevoPrestamo);
                return EstadoPrestamo.EN_CURSO;
            } else {
                System.out.println("El ejemplar no está disponible para préstamo.");
                return estado;
            }
        } else {
            System.out.println("El artículo no se encuentra en la biblioteca.");
            return EstadoPrestamo.ANY_STATE;
        }
    }


    public void mostrarTodosLosPrestamos() {
        if (Principal.enCurso.isEmpty()) {
            System.out.println("No hay préstamos registrados.");
            return;
        }

        System.out.printf("%-10s %-20s %-20s %-15s %-15s%n", "ID", "Usuario", "Artículo", "Fecha Préstamo", "Fecha Devolución");
        System.out.println("---------------------------------------------------------------");

        Principal.enCurso.values().forEach(prestamo -> {
            System.out.printf("%-10d %-20s %-20s %-15s %-15s%n",
                prestamo.getPrestamoId(),
                prestamo.getCliente().getDni(),
                prestamo.getEjemplar().getCod(),
                prestamo.getFechaInicio(),
                prestamo.getFechaFin() != null ? prestamo.getFechaFin() : "Pendiente"
            );
        });
    }

    public void aumentarStock(Ejemplar ejemplar) {
        Principal.inventario.merge(ejemplar, 1, Integer::sum); // Incrementa el stock
        actualizarStockEnLista(ejemplar);
    }

    public void reducirStock(Ejemplar ejemplar) {
        Principal.inventario.computeIfPresent(ejemplar, (key, stock) -> stock > 0 ? stock - 1 : 0);
        actualizarStockEnLista(ejemplar);
    }

    private void actualizarStockEnLista(Ejemplar ejemplar) {
        Principal.articulos.stream()
            .filter(e -> e.equals(ejemplar))
            .findFirst()
            .ifPresent(e -> e.setStock(Principal.inventario.get(ejemplar)));
    }
    
    public <T extends Ejemplar> T seleccionarArticulo(Class<T> clazz, int cod) {
        return Principal.articulos.stream()
            .filter(ejemplar -> clazz.isInstance(ejemplar) && ejemplar.getCod() == cod)
            .map(clazz::cast)
            .findFirst()
            .orElse(null);
    }

    public <T extends Persona> T validarCliente(Class<T> clazz, int id) {
        return Principal.usuarios.stream()
            .filter(persona -> clazz.isInstance(persona) && persona.getId() == id)
            .map(clazz::cast)
            .findFirst()
            .orElse(null);
    }

    // Métodos auxiliares para solicitar datos
	
    private String solicitarTexto(String campo) {
        System.out.println(campo + ": ");
        return sc.next();
    }
    
    private Integer solicitarIdCliente() {
        System.out.print("Código del Cliente solicitado: ");
        int id = sc.nextInt();
        sc.nextLine();  // Limpiar buffer
        return id;
    }

    private void imprimirTicket(Cliente cliente) {
        // Buscar el último préstamo del cliente usando streams
        Principal.enCurso.values().stream()
            .filter(prestamo -> prestamo.getCliente().equals(cliente))  // Filtrar por cliente
            .max(Comparator.comparing(Prestamo::getFechaInicio))        // Obtener el último préstamo
            .ifPresentOrElse(
                prestamo -> { // Si se encuentra el préstamo
                    TicketPrestamo ticket = new TicketPrestamo(
                        Valor.getTk(), cliente,
                        prestamo.getEjemplar(),
                        prestamo.getFechaInicio(),
                        prestamo.getFechaFin()
                    );
                    System.out.println(ticket);  // Imprimir el ticket
                },
                () -> System.out.println("No se encontró ningún préstamo para el cliente.")  // Si no hay préstamo
            );
    }
    
    
    public void aumentarContadorPrestamos(Cliente cliente) {
        int conteoActual = Principal.clientes.getOrDefault(cliente, cliente.getPrestamosRealizados());
        conteoActual++;
        // Actualiza el stock en inventario y articulos
        actualizarContador(cliente, conteoActual);
        System.out.println("Préstamos realizados actualizados: " + conteoActual);
    }

    private void reducirContadorPrestamos(Cliente cliente) {
        // Obtiene el stock actual del inventario
        int conteoActual = Principal.clientes.getOrDefault(cliente, cliente.getPrestamosRealizados());

        if (conteoActual > 0) {
        	conteoActual--;  // Reduce el stock en 1
            // Actualiza el stock en inventario y articulos
            actualizarContador(cliente, conteoActual);
        } else {
            System.out.println("No se pueden reducir los préstamos, ya que el cliente no tiene préstamos activos.");
        }

        System.out.println("Préstamos realizados actualizados: " + conteoActual);
    }
   
    private void actualizarContador(Cliente cliente, int conteoActual) {
    	// Actualiza el contador de préstamos en el mapa
    	Principal.clientes.put(cliente, conteoActual);
    	// También actualiza el contador de préstamos dentro del objeto cliente
    	cliente.setPrestamosRealizados(conteoActual);

        System.out.println("Contador de préstamos actualizado para el cliente: " + cliente.getNombre());
    }

}

