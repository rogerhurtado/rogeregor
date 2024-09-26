package interfaces;

import java.time.LocalDate;

public interface DeudaPrestamo<T, R> {
	// Método para verificar si el préstamo tiene deuda
	boolean test(LocalDate fechaFin);	  
 
}
