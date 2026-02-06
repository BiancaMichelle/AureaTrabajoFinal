package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.Clase;
import com.example.demo.model.Curso;
import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.OfertaAcademicaRepository;

@Service
@Transactional
public class CursoService {

    private final CursoRepository cursoRepository;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final ModuloRepository moduloRepository;

    public CursoService(CursoRepository cursoRepository,
            OfertaAcademicaRepository ofertaAcademicaRepository,
            ModuloRepository moduloRepository) {
        this.cursoRepository = cursoRepository;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.moduloRepository = moduloRepository;
    }

    public Curso obtenerCursoPorId(Long cursoId) {
        Long cursoIdSeguro = Objects.requireNonNull(cursoId, "El identificador del curso no puede ser nulo");

        return cursoRepository.findById(cursoIdSeguro)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));
    }

    public Modulo crearModulo(String nombre, String descripcion, String objetivos, String temario, String bibliografia,
            LocalDate fechaInicio, LocalDate fechaFin,
            Boolean visibilidad, Long cursoId) {
        // ✅ Buscar en OfertaAcademicaRepository para soportar Cursos Y Formaciones
        Long cursoIdSeguro = Objects.requireNonNull(cursoId,
            "El identificador de la oferta académica no puede ser nulo");

        OfertaAcademica curso = ofertaAcademicaRepository.findById(cursoIdSeguro)
                .orElseThrow(() -> new RuntimeException("Oferta académica no encontrada"));
        
        // Validación de Estado de la Oferta (Precondición CU-23: El curso debe estar en estado de alta)
        if (curso.getEstado() != EstadoOferta.ACTIVA && curso.getEstado() != EstadoOferta.ENCURSO) {
            // Asumimos que ACTIVA o ENCURSO son estados válidos para agregar contenido.
             throw new RuntimeException("No se pueden agregar módulos a una oferta que no está activa o en curso.");
        }

        System.out.println(
                "📚 Creando módulo para: " + curso.getNombre() + " (Tipo: " + curso.getClass().getSimpleName() + ")");

        // Validación de datos obligatorios
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del módulo es obligatorio.");
        }
        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias.");
        }
        if (fechaFin.isBefore(fechaInicio)) {
             throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }

        Modulo modulo = new Modulo();
        modulo.setNombre(nombre);
        modulo.setDescripcion(descripcion);
        modulo.setObjetivos(objetivos);
        modulo.setTemario(temario);
        modulo.setBibliografia(bibliografia);
        modulo.setFechaInicioModulo(fechaInicio);
        modulo.setFechaFinModulo(fechaFin);
        modulo.setVisibilidad(visibilidad != null ? visibilidad : true);
        modulo.setCurso(curso);
        modulo.setClases(new ArrayList<>());
        modulo.setActividades(new ArrayList<>());

        return moduloRepository.save(modulo);
    }

    public Modulo actualizarModulo(UUID moduloId, String nombre, String descripcion, String objetivos, String temario, String bibliografia,
            LocalDate fechaInicio, LocalDate fechaFin, Boolean visibilidad, Long cursoId) {
        UUID moduloIdSeguro = Objects.requireNonNull(moduloId,
                "El identificador del módulo es obligatorio");

        Modulo modulo = moduloRepository.findById(moduloIdSeguro)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));

        // Validar que el módulo pertenece al curso indicado
        if (cursoId != null && !modulo.getCurso().getIdOferta().equals(cursoId)) {
             throw new IllegalArgumentException("El módulo no pertenece al curso especificado");
        }

        // Validar Estado del Curso (CU-24)
        EstadoOferta estado = modulo.getCurso().getEstado();
        if (estado != EstadoOferta.ACTIVA && estado != EstadoOferta.ENCURSO) {
            throw new IllegalStateException("El curso no se encuentra en estado ACTIVA o ENCURSO, por lo que no puede ser modificado.");
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del módulo no puede estar vacío");
        }

        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción del módulo no puede estar vacía");
        }

        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("Las fechas del módulo son obligatorias");
        }

        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        
        // Detectar cambios críticos para notificaciones
        boolean fechasCambiadas = !Objects.equals(modulo.getFechaInicioModulo(), fechaInicio) || !Objects.equals(modulo.getFechaFinModulo(), fechaFin);
        // Manejo de null en visibilidad actual
        boolean visibilidadActual = modulo.getVisibilidad() != null ? modulo.getVisibilidad() : false;
        boolean nuevaVisibilidad = visibilidad != null ? visibilidad : false;
        boolean visibilidadCambiada = visibilidadActual != nuevaVisibilidad;

        modulo.setNombre(nombre.trim());
        modulo.setDescripcion(descripcion.trim());
        modulo.setObjetivos(objetivos);
        modulo.setTemario(temario);
        modulo.setBibliografia(bibliografia);

        modulo.setFechaInicioModulo(fechaInicio);
        modulo.setFechaFinModulo(fechaFin);
        modulo.setVisibilidad(nuevaVisibilidad);

        Modulo moduloGuardado = moduloRepository.save(modulo);
        
        if (fechasCambiadas || visibilidadCambiada) {
            notificarCambioModulo(moduloGuardado);
        }
        
        return moduloGuardado;
    }

    private void notificarCambioModulo(Modulo modulo) {
        // TODO: Integrar con sistema real de notificaciones
        System.out.println("Stub Notificación: Se ha modificado el módulo " + modulo.getNombre() + " (ID: " + modulo.getIdModulo() + ")");
    }

    public Modulo actualizarVisibilidadModulo(UUID moduloId, Boolean visibilidad) {
        UUID moduloIdSeguro = Objects.requireNonNull(moduloId, "El identificador del módulo es obligatorio");

        Modulo modulo = moduloRepository.findById(moduloIdSeguro)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));

        modulo.setVisibilidad(Boolean.TRUE.equals(visibilidad));

        return moduloRepository.save(modulo);
    }

    public Modulo obtenerModuloPorId(UUID moduloId) {
        return moduloRepository.findById(moduloId)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));
    }

    @Transactional
    public void eliminarModulo(UUID moduloId) {
        UUID moduloIdSeguro = Objects.requireNonNull(moduloId, "El identificador del módulo es obligatorio");

        Modulo modulo = moduloRepository.findById(moduloIdSeguro)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));

        // Validar Estado del Curso (Precondición CU-25)
        EstadoOferta estado = modulo.getCurso().getEstado();
        if (estado != EstadoOferta.ACTIVA && estado != EstadoOferta.ENCURSO) {
            throw new IllegalStateException("El curso no se encuentra en estado ACTIVA o ENCURSO, por lo que no se pueden eliminar módulos.");
        }

        // RF-02: Verificar que no tenga contenido activo (clases o actividades)
        boolean tieneClases = modulo.getClases() != null && !modulo.getClases().isEmpty();
        boolean tieneActividades = modulo.getActividades() != null && !modulo.getActividades().isEmpty();

        if (tieneClases || tieneActividades) {
            throw new IllegalStateException("No se puede eliminar el módulo porque contiene clases o actividades activas. Elimine el contenido primero.");
        }

        moduloRepository.delete(modulo);
    }

    public Clase crearClase(String titulo, String descripcion, UUID moduloId) {
        UUID moduloIdSeguro = Objects.requireNonNull(moduloId, "El identificador del módulo no puede ser nulo");

        Modulo modulo = moduloRepository.findById(moduloIdSeguro)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));

        Clase clase = new Clase();
        clase.setTitulo(titulo);
        clase.setDescripcion(descripcion);
        clase.setInicio(LocalDateTime.now().plusDays(1));
        clase.setAsistenciaAutomatica(true);
        clase.setPreguntasAleatorias(false);
        clase.setCantidadPreguntas(0);
        clase.setModulo(modulo);
        clase.setCurso(modulo.getCurso());

        // Guardar la clase (necesitarías ClaseRepository)
        // return claseRepository.save(clase);
        return clase;
    }

    /**
     * Obtiene todos los cursos
     */
    public List<Curso> obtenerTodos() {
        return cursoRepository.findAll();
    }

    /**
     * Guarda un curso
     */
    public Curso guardar(Curso curso) {
        // Validar datos antes de guardar
        List<String> errores = curso.validarDatos();
        errores.addAll(curso.validarDatosCurso());

        if (!errores.isEmpty()) {
            throw new IllegalArgumentException("Datos del curso inválidos: " + String.join(", ", errores));
        }

        return cursoRepository.save(curso);
    }

    /**
     * Modifica un curso existente
     */
    public boolean modificar(Long id, Curso datosNuevos) {
        Optional<Curso> cursoOpt = obtenerCursoPorIdOpt(id);

        if (!cursoOpt.isPresent()) {
            return false;
        }

        Curso cursoExistente = cursoOpt.get();

        // Verificar si puede ser editado
        if (!cursoExistente.puedeSerEditada()) {
            return false;
        }

        // Validar nuevos datos
        List<String> errores = datosNuevos.validarDatos();
        errores.addAll(datosNuevos.validarDatosCurso());

        if (!errores.isEmpty()) {
            return false;
        }

        // Aplicar modificaciones
        cursoExistente.modificarDatosBasicos(
                datosNuevos.getNombre(),
                datosNuevos.getDescripcion(),
                datosNuevos.getDuracion(),
                datosNuevos.getFechaInicio(),
                datosNuevos.getFechaFin(),
                datosNuevos.getModalidad(),
                datosNuevos.getCupos(),
                datosNuevos.getVisibilidad(),
                datosNuevos.getCostoInscripcion(),
                datosNuevos.getCostoMora() != null ? datosNuevos.getCostoMora() : 0.0,
                datosNuevos.getCertificado(),
                datosNuevos.getLugar(),
                datosNuevos.getEnlace(),
                datosNuevos.getFechaInicioInscripcion(),
                datosNuevos.getFechaFinInscripcion());

        cursoExistente.modificarDatosCurso(
                datosNuevos.getTemario(),
                datosNuevos.getDocentes(),
                datosNuevos.getCostoCuota(),
                datosNuevos.getCostoMora(),
                datosNuevos.getNrCuotas(),
                datosNuevos.getDiaVencimiento());

        cursoRepository.save(cursoExistente);
        return true;
    }

    /**
     * Modifica un curso con los parámetros del formulario
     */
    public Curso modificar(Long idOferta, String nombre, String descripcion,
            LocalDate fechaInicio, LocalDate fechaFin, Integer cupos,
            Double costoCuota, Double costoMora, Integer nrCuotas,
            Integer diaVencimiento, String modalidad, Boolean otorgaCertificado,
            org.springframework.web.multipart.MultipartFile imagen, String categorias,
            String horarios, String docentes) {
        try {
            // Buscar el curso existente
            Optional<Curso> cursoOpt = obtenerCursoPorIdOpt(idOferta);
            if (!cursoOpt.isPresent()) {
                throw new RuntimeException("Curso no encontrado con ID: " + idOferta);
            }

            Curso cursoExistente = cursoOpt.get();

            // Verificar si puede ser modificado
            if (!cursoExistente.puedeSerEditada()) {
                throw new RuntimeException(
                        "El curso no puede ser modificado porque ya finalizó o tiene inscripciones activas");
            }

            // Modificar datos básicos
            cursoExistente.setNombre(nombre);
            cursoExistente.setDescripcion(descripcion);
            cursoExistente.setFechaInicio(fechaInicio);
            cursoExistente.setFechaFin(fechaFin);
            cursoExistente.setCupos(cupos);
            cursoExistente.setCertificado(otorgaCertificado);

            if (modalidad != null && !modalidad.isEmpty()) {
                cursoExistente.setModalidad(com.example.demo.enums.Modalidad.valueOf(modalidad.toUpperCase()));
            }

            // Modificar datos específicos del curso
            cursoExistente.setCostoCuota(costoCuota);
            cursoExistente.setCostoMora(costoMora);
            cursoExistente.setNrCuotas(nrCuotas);
            cursoExistente.setDiaVencimiento(diaVencimiento);

            // TODO: Manejar imagen, categorías, horarios y docentes según sea necesario
            // Estas funcionalidades pueden agregarse después si es necesario

            // Guardar cambios
            return cursoRepository.save(cursoExistente);

        } catch (Exception e) {
            System.err.println("Error al modificar curso: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Cambia el estado de un curso - usa método heredado
     */
    public boolean cambiarEstado(Long id, EstadoOferta nuevoEstado) {
        Optional<Curso> cursoOpt = obtenerCursoPorIdOpt(id);

        if (!cursoOpt.isPresent()) {
            return false;
        }

        Curso curso = cursoOpt.get();
        boolean exito = curso.cambiarEstado(nuevoEstado); // Método heredado de OfertaAcademica

        if (exito) {
            cursoRepository.save(curso);
        }

        return exito;
    }

    /**
     * Da de baja un curso - usa método heredado
     */
    public boolean darDeBaja(Long id) {
        return cambiarEstado(id, EstadoOferta.DE_BAJA);
    }

    /**
     * Da de alta un curso - usa método heredado
     */
    public boolean darDeAlta(Long id) {
        return cambiarEstado(id, EstadoOferta.ACTIVA);
    }

    /**
     * Elimina un curso con validaciones - usa método heredado
     */
    public boolean eliminar(Long id) {
        Long cursoIdSeguro = Objects.requireNonNull(id, "El identificador del curso no puede ser nulo");

        Optional<Curso> cursoOpt = obtenerCursoPorIdOpt(cursoIdSeguro);

        if (!cursoOpt.isPresent()) {
            return false;
        }

        Curso curso = cursoOpt.get();

        if (!curso.puedeSerEliminada()) { // Método heredado de OfertaAcademica
            return false;
        }

        cursoRepository.deleteById(cursoIdSeguro);
        return true;
    }

    /**
     * Obtiene un curso por ID (devuelve Optional)
     */
    public Optional<Curso> obtenerCursoPorIdOpt(Long id) {
        Long cursoIdSeguro = Objects.requireNonNull(id, "El identificador del curso no puede ser nulo");
        return cursoRepository.findById(cursoIdSeguro);
    }

    /**
     * Obtiene cursos por estado - usa repositorio directamente
     */
    public List<Curso> obtenerPorEstado(EstadoOferta estado) {
        return cursoRepository.findByEstado(estado);
    }

    /**
     * Busca cursos por nombre - usa repositorio directamente
     */
    public List<Curso> buscarPorNombre(String nombre) {
        return cursoRepository.findByNombreContainingIgnoreCase(nombre);
    }
}