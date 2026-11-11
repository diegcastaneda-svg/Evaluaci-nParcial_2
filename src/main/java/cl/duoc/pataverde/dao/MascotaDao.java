package cl.duoc.pataverde.dao;

import cl.duoc.pataverde.model.Mascota;

import java.io.IOException;
import java.util.List;

/**
 * Contrato de acceso a datos de las mascotas.
 * El controlador depende de esta interfaz, no de cómo se guardan los datos.
 */
public interface MascotaDao {

    List<Mascota> listar() throws IOException;

    void agregar(Mascota mascota) throws IOException;

    void eliminar(Mascota mascota) throws IOException;
}
