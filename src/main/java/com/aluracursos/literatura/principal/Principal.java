package com.aluracursos.literatura.principal;

import com.aluracursos.literatura.model.*;
import com.aluracursos.literatura.repository.AutorRepository;
import com.aluracursos.literatura.service.ConsumoAPI;
import com.aluracursos.literatura.service.ConvierteDatos;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private static final String URL_BASE = "https://gutendex.com/books/";
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private ConvierteDatos conversor = new ConvierteDatos();
    private Scanner teclado = new Scanner(System.in);
    private AutorRepository repository;
    public Principal(AutorRepository repository){
        this.repository = repository;
    }


    public void muestraElMenu() {
        var opcion = -1;
        var menu = """
                 \n --- Sea bienvenido/a al sistema de Literalura ---
                ...................................................
                             MENU PRINCIPAL
                 --------------------------------------------------
                 1.- Buscar libros por título
                 2.- Listar Libros registrados
                 3.- Listar autores registrados
                 4.- Listar autores vivos a partir de un determinado año
                 5.- Listar libros por idioma
                 6.- Top 10 libros mas descargados
                 7.- Estadísticas
                 
                 0.- Salir del programa
                 
                 Elija la opción a través de su número:   
                 """;

        while (opcion != 0) {
            System.out.println(menu);
            try {
                opcion = Integer.valueOf(teclado.nextLine());
                switch (opcion) {
                    case 1:
                        buscarLibroPorTitulo();
                        break;
                    case 2:
                        listarLibrosRegistrados();
                        break;
                    case 3:
                        listarAutoresRegistrados();
                        break;
                    case 4:
                        listarAutoresVivos();
                        break;
                    case 5:
                        listarLibrosPorIdioma();
                        break;
                    case 6:
                        top10Libros();
                        break;
                    case 7:
                        generarEstadisticas();
                        break;
                    case 0:
                        System.out.println("Gracias por utilizar Literalura");
                        System.out.println("Saliendo de la aplicación  ...");
                        break;
                    default:
                        System.out.println("¡Opción no válida!");
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Opción no válida: " + e.getMessage());

            }
        }
    }

        public void buscarLibroPorTitulo() {
            System.out.println("""
            
             ******* BUSCAR LIBROS POR TÍTULO *******
             """);
            System.out.println("Introduzca el nombre del libro que desea buscar:");
            var nombre = teclado.nextLine();
            var json = consumoAPI.obtenerDatos(URL_BASE + "?search=" + nombre.replace(" ", "+").toLowerCase());

            // Check if JSON is empty
            if (json.isEmpty() || !json.contains("\"count\":0,\"next\":null,\"previous\":null,\"results\":[]")) {
                var datos = conversor.obtenerDatos(json, Datos.class);

                // Process valid data
                Optional<DatosLibros> libroBuscado = datos.libros().stream()
                        .findFirst();
                if (libroBuscado.isPresent()) {
                    System.out.println(
                            "\n------------- LIBRO  --------------" +
                                    "\nTítulo: " + libroBuscado.get().titulo() +
                                    "\nAutor: " + libroBuscado.get().autores().stream()
                                    .map(a -> a.nombre()).limit(1).collect(Collectors.joining()) +
                                    "\nIdioma: " + libroBuscado.get().idiomas().stream().collect(Collectors.joining()) +
                                    "\nNúmero de descargas: " + libroBuscado.get().numeroDeDescargas() +
                                    "\n--------------------------------------\n"
                    );

                    try {
                        List<Libro> libroEncontrado = libroBuscado.stream().map(a -> new Libro(a)).collect(Collectors.toList());
                        Autor autorAPI = libroBuscado.stream().
                                flatMap(l -> l.autores().stream()
                                        .map(a -> new Autor(a)))
                                .collect(Collectors.toList()).stream().findFirst().get();
                        Optional<Autor> autorBD = repository.buscarAutorPorNombre(libroBuscado.get().autores().stream()
                                .map(a -> a.nombre())
                                .collect(Collectors.joining()));
                        Optional<Libro> libroOptional = repository.buscarLibroPorNombre(nombre);
                        if (libroOptional.isPresent()) {
                            System.out.println("El libro ya está guardado en la BD.");
                        } else {
                            Autor autor;
                            if (autorBD.isPresent()) {
                                autor = autorBD.get();
                                System.out.println("EL autor ya esta guardado en la BD");
                            } else {
                                autor = autorAPI;
                                repository.save(autor);
                            }
                            autor.setLibros(libroEncontrado);
                            repository.save(autor);
                        }
                    } catch (Exception e) {
                        System.out.println("Warning! " + e.getMessage());
                    }
                } else {
                    System.out.println("Libro no encontrado!");
                }
            }
        }

    public void listarLibrosRegistrados () {

        List<Libro> libros = repository.buscarTodosLosLibros();
        System.out.println("""
                    
                     ******* LISTAR LIBROS REGISTRADOS ******
                     """);
        libros.forEach(l -> System.out.println(
                "--------------  LIBRO  -----------------" +
                        "\nTítulo: " + l.getTitulo() +
                        "\nAutor: " + l.getAutor().getNombre() +
                        "\nIdioma: " + l.getIdioma().getIdioma() +
                        "\nNúmero de descargas: " + l.getDescargas() +
                        "\n----------------------------------------\n"
        ));
    }

    public void listarAutoresRegistrados () {

        List<Autor> autores = repository.findAll();
        System.out.println("""
                    
                     ***** LISTAR AUTORES REGISTRADOS *****                    
                     """);
        System.out.println();
        autores.forEach(l -> System.out.println(
                "----------- AUTOR: "+ l.getNombre() + " --------------" +
                        "\nFecha de Nacimiento: " + l.getNacimiento() +
                        "\nFecha de Fallecimiento: " + l.getFallecimiento() +
                        "\nLibros: " + l.getLibros().stream()
                        .map(t -> t.getTitulo()).collect(Collectors.toList()) +
                "\n-------------------------------------------\n"
        ));
    }

    public void listarAutoresVivos () {
        System.out.println("""
                    
                    ***** LISTAR AUTORES VIVOS A PARTIR DE UN DETERMINADO AÑO *****
                     """);
        System.out.println("Introduzca un año para verificar el autor(es) que desea buscar:");
        try {
            var fecha = Integer.valueOf(teclado.nextLine());
            List<Autor> autores = repository.buscarAutoresVivos(fecha);
            if (!autores.isEmpty()) {
                System.out.println();
                autores.forEach(a -> System.out.println(
                        "----------- AUTOR: " + a.getNombre() + " --------------" +
                                "\nFecha de Nacimiento: " + a.getNacimiento() +
                                "\nFecha de Fallecimiento: " + a.getFallecimiento() +
                                "\nLibros: " + a.getLibros().stream()
                                .map(l -> l.getTitulo()).collect(Collectors.toList()) +
                                "\n-------------------------------------------\n"
                ));
            } else {
                System.out.println("No hay autores vivos en el año registrado");
            }
        } catch (NumberFormatException e) {
            System.out.println("Ingresa un año válido " + e.getMessage());
        }
    }

    public void listarLibrosPorIdioma() {
        System.out.println("""
                
                ****** LISTAR LIBROS POR IDIOMA *****         
                """);
        var menu = """
                    ---------------------------------------------------
                    Seleccione el idioma del libro que desea encontrar:
                    ---------------------------------------------------
                    1 - Español
                    2 - Francés
                    3 - Inglés
                    4 - Portugués
                    ----------------------------------------------------
                    """;
        System.out.println(menu);

        try {
            var opcion = Integer.parseInt(teclado.nextLine());

            switch (opcion) {
                case 1:
                    buscarLibrosPorIdioma("es");
                    break;
                case 2:
                    buscarLibrosPorIdioma("fr");
                    break;
                case 3:
                    buscarLibrosPorIdioma("en");
                    break;
                case 4:
                    buscarLibrosPorIdioma("pt");
                    break;
                default:
                    System.out.println("Opción inválida!");
                    break;
            }
        } catch (NumberFormatException e) {
            System.out.println("Opción no válida: " + e.getMessage());
        }
    }

    private void buscarLibrosPorIdioma(String idioma) {
        try {
            Idioma idiomaEnum = Idioma.valueOf(idioma.toUpperCase());
            List<Libro> libros = repository.buscarLibrosPorIdioma(idiomaEnum);
            if (libros.isEmpty()) {
                System.out.println("No hay libros registrados en ese idioma");
            } else {
                System.out.println();
                libros.forEach(l -> System.out.println(
                        "----------- LIBRO   --------------" +
                                "\nTítulo: " + l.getTitulo() +
                                "\nAutor: " + l.getAutor().getNombre() +
                                "\nIdioma: " + l.getIdioma().getIdioma() +
                                "\nNúmero de descargas: " + l.getDescargas() +
                                "\n----------------------------------------\n"
                ));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Introduce un idioma válido en el formato especificado.");
        }
    }

    public void top10Libros () {

        List<Libro> libros = repository.top10Libros();
        System.out.println("""
                    
                     ******* TOP 10 LIBROS MÁS BUSCADOS ********
                     """);
        libros.forEach(l -> System.out.println(
                "----------------- LIBRO ----------------" +
                        "\nTítulo: " + l.getTitulo() +
                        "\nAutor: " + l.getAutor().getNombre() +
                        "\nIdioma: " + l.getIdioma().getIdioma() +
                        "\nNúmero de descargas: " + l.getDescargas() +
                        "\n-------------------------------------------\n"
        ));
    }
    public void generarEstadisticas () {

        var json = consumoAPI.obtenerDatos(URL_BASE);
        var datos = conversor.obtenerDatos(json, Datos.class);
        IntSummaryStatistics est = datos.libros().stream()
                .filter(l -> l.numeroDeDescargas() > 0)
                .collect(Collectors.summarizingInt(DatosLibros::numeroDeDescargas));
        Integer media = (int) est.getAverage();
        System.out.println("\n--------- ESTADÍSTICAS ------------");
        System.out.println("Cantidad media de descargas: " + media);
        System.out.println("Cantidad máxima de descargas: " + est.getMax());
        System.out.println("Cantidad mínima de descargas: " + est.getMin());
        System.out.println("Cantidad de registros evaluados para calcular las estadísticas: " + est.getCount());
        System.out.println("---------------------------------------------------\n");
    }

    //Trabajando con estadísticas
//        DoubleSummaryStatistics est = datos.resultados().stream()
//                .filter(d -> d.numeroDeDescargas() >0 )
//                .collect(Collectors.summarizingDouble(DatosLibros::numeroDeDescargas));
//        System.out.println("Cantidad media de descargas: " + est.getAverage() );
//        System.out.println("Cantidad máxima de descargas: " + est.getMax());
//        System.out.println("cantidad mínima de descargas: " + est.getMin());
//        System.out.println("Cantidad de resgistros evaluados para calcular las estadísticas: " + est.getCount());








//        var json = consumoAPI.obtenerDatos(URL_BASE);
//        System.out.println(json);
//        var datos = conversor.obtenerDatos(json, Datos.class);
//        System.out.println(datos);
//
////        //Top 10 libros mas descargados
////        System.out.println("Top 10 libros mas descargados");
////        datos.resultados().stream()
////                .sorted(Comparator.comparing(DatosLibros::numeroDeDescargas).reversed())
////                .limit(10)
////                //para realizar la transformacion a mayusculas se va a agarrar esa instancia de libro(l), l.titulo y lo convierte en uppercase
////                .map(l -> l.titulo().toUpperCase())
////                .forEach(System.out::println);
//
//        //Búsqueda de libros por nombre
//        System.out.println("Ingrese el nombre del libro que desea buscar");
//        var tituloLibro = teclado.nextLine();
//        json = consumoAPI.obtenerDatos(URL_BASE+"?search=" + tituloLibro.replace(" ","+"));
//        var datosBusqueda = conversor.obtenerDatos(json, Datos.class);
//        Optional<DatosLibros> libroBuscado = datosBusqueda.resultados().stream()
//                .filter(l -> l.titulo().toUpperCase().contains(tituloLibro.toUpperCase()))
//                .findFirst();
//        if (libroBuscado.isPresent()){
//            System.out.println("Libro encontrado");
//            System.out.println(libroBuscado.get());
//        }else {
//            System.out.println("Libro no encontrado");
//        }
//
//        //Trabajando con estadísticas
//        DoubleSummaryStatistics est = datos.resultados().stream()
//                .filter(d -> d.numeroDeDescargas() >0 )
//                .collect(Collectors.summarizingDouble(DatosLibros::numeroDeDescargas));
//        System.out.println("Cantidad media de descargas: " + est.getAverage() );
//        System.out.println("Cantidad máxima de descargas: " + est.getMax());
//        System.out.println("cantidad mínima de descargas: " + est.getMin());
//        System.out.println("Cantidad de resgistros evaluados para calcular las estadísticas: " + est.getCount());


    }
