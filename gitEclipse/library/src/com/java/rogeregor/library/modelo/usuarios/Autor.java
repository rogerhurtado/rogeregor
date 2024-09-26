package com.java.rogeregor.library.modelo.usuarios;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.java.rogeregor.library.modelo.articulos.Libro;

public class Autor extends Persona {
	
	    private List<Libro> librosDeAutor = new ArrayList<>();
	    private String paisNacimiento;
	    
	    public Autor() {
	    }

		public Autor(String nombre, String apellido) {
	        super(nombre, apellido);
	    }
	    
	    public Autor(String nombre, String apellido, String paisNacimiento) {
			super(nombre, apellido);
			this.paisNacimiento = paisNacimiento;
		}

		public Autor(String nombre, String apellido, String paisNacimiento, List<Libro> librosDeAutor) {
			super(nombre, apellido);
			this.librosDeAutor = librosDeAutor;
			this.paisNacimiento = paisNacimiento;
		}

		public String getPaisNacimiento() {
			return paisNacimiento;
		}

		public void setPaisNacimiento(String paisNacimiento) {
			this.paisNacimiento = paisNacimiento;
		}
	   
		public List<Libro> getLibrosDeAutor() {
			return librosDeAutor;
		}

		public void setLibrosDeAutor(List<Libro> librosDeAutor) {
			this.librosDeAutor = librosDeAutor;
		}

		@Override
		public String toString() {
			return "Autor [Id=" + getId() + 
					", Nombre=" + getNombre() + 
					", Apellido=" + getApellido() + 
					", PaisNacimiento=" + getPaisNacimiento() + 
					", LibrosDeAutor=" + getLibrosDeAutor() + 
					"]";
		}

		@Override
		public int hashCode() {
			return Objects.hash(paisNacimiento);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Autor other = (Autor) obj;
			return Objects.equals(paisNacimiento, other.paisNacimiento);
		}


}
