package cl.duoc.pataverde.controller;

import cl.duoc.pataverde.dao.MascotaDao;
import cl.duoc.pataverde.dao.MascotaJsonDao;
import cl.duoc.pataverde.model.Gato;
import cl.duoc.pataverde.model.Mascota;
import cl.duoc.pataverde.model.Perro;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainController {

    private static final DecimalFormat FORMATO_PESOS =
            new DecimalFormat("$#,##0", DecimalFormatSymbols.getInstance(Locale.of("es", "CL")));

    @FXML private ComboBox<String> cmbTipo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtEdad;
    @FXML private TextField txtPeso;
    @FXML private TextField txtRaza;
    @FXML private CheckBox chkVacunado;
    @FXML private CheckBox chkAdopcion;
    @FXML private CheckBox chkExterior;

    @FXML private TableView<Mascota> tablaMascotas;
    @FXML private TableColumn<Mascota, String> colTipo;
    @FXML private TableColumn<Mascota, String> colNombre;
    @FXML private TableColumn<Mascota, Number> colEdad;
    @FXML private TableColumn<Mascota, Number> colPeso;
    @FXML private TableColumn<Mascota, String> colCosto;

    @FXML private Label lblMensaje;

    private final MascotaDao dao = new MascotaJsonDao();
    private final ObservableList<Mascota> mascotas = FXCollections.observableArrayList();

    /** JavaFX la llama sola al cargar el FXML. */
    @FXML
    private void initialize() {
        cmbTipo.setItems(FXCollections.observableArrayList("Perro", "Gato"));
        cmbTipo.valueProperty().addListener((obs, anterior, nuevo) -> actualizarCamposEspecificos(nuevo));
        actualizarCamposEspecificos(null);

        configurarTabla();
        cargarMascotas();
    }

    // ---------- Configuración inicial ----------

    private void configurarTabla() {
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo()));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colEdad.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getEdad()));
        colPeso.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getPeso()));
        colCosto.setCellValueFactory(c -> new SimpleStringProperty(
                FORMATO_PESOS.format(Math.round(c.getValue().calcularCostoConsulta()))));
        tablaMascotas.setItems(mascotas);
    }

    private void cargarMascotas() {
        try {
            mascotas.addAll(dao.listar());
        } catch (IOException e) {
            mostrarMensaje("Error al leer los archivos: " + e.getMessage());
        }
    }

    /** Habilita solo los campos del tipo elegido. */
    private void actualizarCamposEspecificos(String tipo) {
        boolean esPerro = "Perro".equals(tipo);
        boolean esGato = "Gato".equals(tipo);
        txtRaza.setDisable(!esPerro);
        chkVacunado.setDisable(!esPerro);
        chkAdopcion.setDisable(!esPerro);
        chkExterior.setDisable(!esGato);
    }

    // ---------- Acciones de los botones ----------

    @FXML
    private void onRegistrar() {
        String tipo = cmbTipo.getValue();
        if (tipo == null) {
            mostrarMensaje("Tipo: debe seleccionar Perro o Gato.");
            return;
        }

        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            mostrarMensaje("Nombre: no puede estar vacío ni contener solo espacios.");
            return;
        }

        int edad;
        try {
            edad = Integer.parseInt(txtEdad.getText().trim());
        } catch (NumberFormatException e) {
            mostrarMensaje("Edad: debe ser un número entero.");
            return;
        }
        if (edad < 0 || edad > 30) {
            mostrarMensaje("Edad: debe estar entre 0 y 30 años.");
            return;
        }

        double peso;
        try {
            peso = Double.parseDouble(txtPeso.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            mostrarMensaje("Peso: debe ser un número (se aceptan decimales).");
            return;
        }
        if (!(peso > 0) || Double.isInfinite(peso)) {
            mostrarMensaje("Peso: debe ser mayor que cero.");
            return;
        }

        Mascota mascota;
        try {
            if ("Perro".equals(tipo)) {
                String raza = txtRaza.getText().trim();
                if (raza.isEmpty()) {
                    mostrarMensaje("Raza: no puede estar vacía ni contener solo espacios.");
                    return;
                }
                Perro perro = new Perro(nombre, edad, peso, raza, chkVacunado.isSelected());
                if (chkAdopcion.isSelected()) {
                    perro.marcarDisponibleParaAdopcion();
                }
                mascota = perro;
            } else {
                mascota = new Gato(nombre, edad, peso, chkExterior.isSelected());
            }
        } catch (IllegalArgumentException e) {
            mostrarMensaje(e.getMessage()); // el modelo explica el motivo
            return;
        }

        try {
            dao.agregar(mascota);
        } catch (IOException e) {
            mostrarMensaje("Error al guardar los archivos: " + e.getMessage());
            return;
        }

        mascotas.add(mascota);
        mostrarMensaje(mascota.getNombre() + " (" + mascota.getTipo() + ") registrado correctamente.");
        limpiarFormulario();
    }

    @FXML
    private void onEliminar() {
        Mascota seleccionada = tablaMascotas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje("Debe seleccionar una mascota de la tabla para eliminarla.");
            return;
        }

        try {
            dao.eliminar(seleccionada);
        } catch (IOException e) {
            mostrarMensaje("Error al guardar los archivos: " + e.getMessage());
            return;
        }

        mascotas.remove(seleccionada);
        mostrarMensaje(seleccionada.getNombre() + " (" + seleccionada.getTipo() + ") eliminado correctamente.");
    }

    // ---------- Auxiliares ----------

    private void mostrarMensaje(String texto) {
        lblMensaje.setText(texto); // setText reemplaza el mensaje anterior
    }

    private void limpiarFormulario() {
        cmbTipo.setValue(null);
        txtNombre.clear();
        txtEdad.clear();
        txtPeso.clear();
        txtRaza.clear();
        chkVacunado.setSelected(false);
        chkAdopcion.setSelected(false);
        chkExterior.setSelected(false);
    }
}
