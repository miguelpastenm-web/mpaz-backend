package cl.duoc.mpazpro.contenidos.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cl.duoc.mpazpro.contenidos.dto.ObjetivoRequest;
import cl.duoc.mpazpro.contenidos.exception.RecursoNoEncontradoException;
import cl.duoc.mpazpro.contenidos.exception.ReglaNegocioException;
import cl.duoc.mpazpro.contenidos.model.Objetivo;
import cl.duoc.mpazpro.contenidos.repository.ActividadRepository;
import cl.duoc.mpazpro.contenidos.repository.ObjetivoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjetivoServiceTest {

    @Mock
    private ObjetivoRepository objetivoRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @InjectMocks
    private ObjetivoService service;

    @Test
    void listar_devuelveLosObjetivosDelRepositorio() {
        List<Objetivo> objetivos = List.of(objetivoConId(1), objetivoConId(2));
        when(objetivoRepository.findAll()).thenReturn(objetivos);

        assertEquals(objetivos, service.listar());
    }

    @Test
    void obtener_devuelveElObjetivoSolicitado() {
        Objetivo objetivo = objetivoConId(5);
        when(objetivoRepository.findById(5)).thenReturn(Optional.of(objetivo));

        assertSame(objetivo, service.obtener(5));
    }

    @Test
    void obtener_lanzaExcepcionCuandoElObjetivoNoExiste() {
        when(objetivoRepository.findById(5)).thenReturn(Optional.empty());

        RecursoNoEncontradoException excepcion =
                assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(5));

        assertEquals("El objetivo 5 no existe", excepcion.getMessage());
    }

    @Test
    void crear_copiaLosDatosYDevuelveElObjetivoGuardado() {
        ObjetivoRequest request = new ObjetivoRequest(2, "LE02 OA 01", "Leer palabras", true);
        Objetivo guardado = objetivoConId(5);
        when(objetivoRepository.save(any(Objetivo.class))).thenReturn(guardado);
        ArgumentCaptor<Objetivo> captor = ArgumentCaptor.forClass(Objetivo.class);

        assertSame(guardado, service.crear(request));

        verify(objetivoRepository).save(captor.capture());
        Objetivo objetivo = captor.getValue();
        assertAll(
                () -> assertNull(objetivo.getId()),
                () -> assertEquals(request.asignaturaId(), objetivo.getAsignaturaId()),
                () -> assertEquals(request.codigoMineduc(), objetivo.getCodigoMineduc()),
                () -> assertEquals(request.descripcion(), objetivo.getDescripcion()),
                () -> assertTrue(objetivo.getAutorizado()));
    }

    @Test
    void actualizar_conservaElIdYGuardaLosDatosNuevos() {
        Objetivo existente = objetivoConId(5);
        existente.setAutorizado(true);
        ObjetivoRequest request = new ObjetivoRequest(3, "MA02 OA 01", "Contar objetos", null);
        when(objetivoRepository.findById(5)).thenReturn(Optional.of(existente));
        when(objetivoRepository.save(existente)).thenReturn(existente);

        assertSame(existente, service.actualizar(5, request));

        verify(objetivoRepository).save(existente);
        assertAll(
                () -> assertEquals(5, existente.getId()),
                () -> assertEquals(request.asignaturaId(), existente.getAsignaturaId()),
                () -> assertEquals(request.codigoMineduc(), existente.getCodigoMineduc()),
                () -> assertEquals(request.descripcion(), existente.getDescripcion()),
                () -> assertFalse(existente.getAutorizado()));
    }

    @Test
    void actualizar_rechazaElObjetivoInexistenteSinGuardar() {
        ObjetivoRequest request = new ObjetivoRequest(3, "MA02 OA 01", "Contar objetos", false);
        when(objetivoRepository.findById(5)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(5, request));

        verify(objetivoRepository).findById(5);
        verifyNoMoreInteractions(objetivoRepository);
    }

    @Test
    void eliminar_borraElObjetivoSinActividades() {
        Objetivo objetivo = objetivoConId(5);
        when(objetivoRepository.findById(5)).thenReturn(Optional.of(objetivo));
        when(actividadRepository.existsByOaId(5)).thenReturn(false);

        service.eliminar(5);

        verify(actividadRepository).existsByOaId(5);
        verify(objetivoRepository).delete(objetivo);
    }

    @Test
    void eliminar_rechazaElObjetivoConActividadesAsociadas() {
        when(objetivoRepository.findById(5)).thenReturn(Optional.of(objetivoConId(5)));
        when(actividadRepository.existsByOaId(5)).thenReturn(true);

        ReglaNegocioException excepcion =
                assertThrows(ReglaNegocioException.class, () -> service.eliminar(5));

        assertEquals("No se puede eliminar el objetivo 5 porque tiene actividades asociadas",
                excepcion.getMessage());
        verify(objetivoRepository).findById(5);
        verifyNoMoreInteractions(objetivoRepository);
    }

    @Test
    void eliminar_lanzaExcepcionCuandoElObjetivoNoExiste() {
        when(objetivoRepository.findById(5)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(5));

        verify(objetivoRepository).findById(5);
        verifyNoMoreInteractions(objetivoRepository);
        verifyNoInteractions(actividadRepository);
    }

    private Objetivo objetivoConId(int id) {
        Objetivo objetivo = new Objetivo();
        objetivo.setId(id);
        return objetivo;
    }
}
