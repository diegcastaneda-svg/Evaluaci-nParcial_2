package cl.duoc.pataverde.dao;

import cl.duoc.pataverde.model.Gato;
import cl.duoc.pataverde.model.Mascota;
import cl.duoc.pataverde.model.Perro;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class MascotaJsonDao implements MascotaDao {

    private static final Path ARCHIVO_PERROS = Path.of("perros.json");
    private static final Path ARCHIVO_GATOS = Path.of("gatos.json");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<Mascota> mascotas = new ArrayList<>();

    @Override
    public List<Mascota> listar() throws IOException {
        mascotas.clear();
        mascotas.addAll(leer(ARCHIVO_PERROS, Perro.class));
        mascotas.addAll(leer(ARCHIVO_GATOS, Gato.class));
        return new ArrayList<>(mascotas);
    }

    @Override
    public void agregar(Mascota mascota) throws IOException {
        mascotas.add(mascota);
        try {
            guardar();
        } catch (IOException e) {
            mascotas.remove(mascota); // si no se pudo guardar, no queda en memoria
            throw e;
        }
    }

    @Override
    public void eliminar(Mascota mascota) throws IOException {
        if (!mascotas.remove(mascota)) {
            return;
        }
        try {
            guardar();
        } catch (IOException e) {
            mascotas.add(mascota);
            throw e;
        }
    }

    private <T extends Mascota> List<T> leer(Path ruta, Class<T> clase) throws IOException {
        List<T> resultado = new ArrayList<>();
        if (!Files.exists(ruta)) {
            return resultado;
        }
        try (Reader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            JsonArray arreglo = gson.fromJson(lector, JsonArray.class);
            if (arreglo != null) {
                for (JsonElement elemento : arreglo) {
                    resultado.add(gson.fromJson(elemento, clase));
                }
            }
        } catch (JsonParseException e) {
            throw new IOException("El archivo " + ruta + " tiene un formato no válido.", e);
        }
        return resultado;
    }

    private void guardar() throws IOException {
        List<Perro> perros = new ArrayList<>();
        List<Gato> gatos = new ArrayList<>();
        for (Mascota m : mascotas) {
            if (m instanceof Perro p) {
                perros.add(p);
            } else if (m instanceof Gato g) {
                gatos.add(g);
            }
        }
        escribir(ARCHIVO_PERROS, perros);
        escribir(ARCHIVO_GATOS, gatos);
    }

    private void escribir(Path ruta, List<?> datos) throws IOException {
        try (Writer escritor = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            gson.toJson(datos, escritor);
        }
    }
}
