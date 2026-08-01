package com.example.despensacx;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.despensacx.databinding.ActivityEstadisticasBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class EstadisticasActivity extends AppCompatActivity {

    private ActivityEstadisticasBinding binding;
    private AppDatabase db;
    private AnioAdapter adapterAnio;
    private MesAdapter adapterMes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEstadisticasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarEstadisticas);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Estadísticas por Año");
        }

        db = AppDatabase.getInstance(this);
        binding.rvEstadisticas.setLayoutManager(new LinearLayoutManager(this));

        cargarDatosEstadisticas();
    }

    private void cargarDatosEstadisticas() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ListaEntity> listasActivas = db.listaDao().getListasActivasSync();
            List<ProductoEntity> productos = db.productoDao().getAllSync();
            List<TiendaEntity> tiendas = db.tiendaDao().getAllTiendasSync();

            Map<Long, String> mapaTiendas = new HashMap<>();
            for (TiendaEntity t : tiendas) {
                mapaTiendas.put(t.getId(), t.getNombre());
            }

            // Agrupar Totales por Lista y por Tienda
            Map<Long, Map<String, Double>> productosPorListaYTienda = new HashMap<>();
            Map<Long, Double> totalPorLista = new HashMap<>();

            for (ProductoEntity p : productos) {
                if (!p.isSeleccionado()) continue;

                long lId = p.getListaId();
                double subtotal = p.getPrecio() * p.getCantidad();
                String nombreTienda = mapaTiendas.getOrDefault(p.getTiendaId(), "Otra");

                totalPorLista.put(lId, totalPorLista.getOrDefault(lId, 0.0) + subtotal);

                productosPorListaYTienda.putIfAbsent(lId, new HashMap<>());
                Map<String, Double> mTiendas = productosPorListaYTienda.get(lId);
                mTiendas.put(nombreTienda, mTiendas.getOrDefault(nombreTienda, 0.0) + subtotal);
            }

            // Agrupar por Año -> Mes
            Map<String, Map<String, EstadisticaModel.MesModel>> estructuraAnios = new TreeMap<>(Collections.reverseOrder());

            // Permite parsear tanto "yyyy-MM-dd" como "yyyy-MM-dd HH:mm"
            SimpleDateFormat fmtLecturaFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat fmtAnio = new SimpleDateFormat("yyyy", Locale.getDefault());
            SimpleDateFormat fmtMesClave = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            SimpleDateFormat fmtMesNombre = new SimpleDateFormat("MMMM", new Locale("es", "ES"));

            for (ListaEntity lista : listasActivas) {
                try {
                    String fechaStr = lista.getFechaCreacion();
                    Date fecha = null;
                    if (fechaStr != null && !fechaStr.isEmpty()) {
                        fecha = fmtLecturaFecha.parse(fechaStr);
                    }
                    if (fecha == null) fecha = new Date();

                    String anioStr = fmtAnio.format(fecha);
                    String mesClave = fmtMesClave.format(fecha);
                    String mesNombre = fmtMesNombre.format(fecha);

                    // Capitalizar primera letra del mes (ej. "Agosto")
                    if (!mesNombre.isEmpty()) {
                        mesNombre = mesNombre.substring(0, 1).toUpperCase() + mesNombre.substring(1);
                    }

                    double totalLista = totalPorLista.getOrDefault(lista.getId(), 0.0);
                    Map<String, Double> desglosTiendasLista = productosPorListaYTienda.getOrDefault(lista.getId(), new HashMap<>());

                    estructuraAnios.putIfAbsent(anioStr, new HashMap<>());
                    Map<String, EstadisticaModel.MesModel> mesesDelAnio = estructuraAnios.get(anioStr);

                    if (mesesDelAnio.containsKey(mesClave)) {
                        // Consolidación de múltiples listas creadas en el mismo mes
                        EstadisticaModel.MesModel mesExistente = mesesDelAnio.get(mesClave);
                        double nuevoTotalMes = mesExistente.getTotalMes() + totalLista;
                        Map<String, Double> tiendasConsolidadas = mesExistente.getGastosPorTienda();

                        for (Map.Entry<String, Double> entry : desglosTiendasLista.entrySet()) {
                            tiendasConsolidadas.put(entry.getKey(), tiendasConsolidadas.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
                        }

                        mesesDelAnio.put(mesClave, new EstadisticaModel.MesModel(mesNombre, mesClave, nuevoTotalMes, tiendasConsolidadas));
                    } else {
                        mesesDelAnio.put(mesClave, new EstadisticaModel.MesModel(mesNombre, mesClave, totalLista, new HashMap<>(desglosTiendasLista)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Convertir Estructura a Objetos Finales
            List<EstadisticaModel.AnnoModel> listaAnios = new ArrayList<>();
            for (Map.Entry<String, Map<String, EstadisticaModel.MesModel>> entryAnio : estructuraAnios.entrySet()) {
                EstadisticaModel.AnnoModel anioObj = new EstadisticaModel.AnnoModel(entryAnio.getKey());
                double totalAcumuladoAnio = 0.0;

                for (EstadisticaModel.MesModel mesObj : entryAnio.getValue().values()) {
                    totalAcumuladoAnio += mesObj.getTotalMes();
                    anioObj.getMeses().add(mesObj);
                }

                anioObj.setTotalAnio(totalAcumuladoAnio);
                listaAnios.add(anioObj);
            }

            runOnUiThread(() -> mostrarAnios(listaAnios));
        });
    }

    private void mostrarAnios(List<EstadisticaModel.AnnoModel> anios) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Estadísticas por Año");
        }
        adapterAnio = new AnioAdapter(anios, this::mostrarMeses);
        binding.rvEstadisticas.setAdapter(adapterAnio);
    }

    private void mostrarMeses(EstadisticaModel.AnnoModel anio) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Meses - " + anio.getAnio());
        }
        adapterMes = new MesAdapter(anio.getMeses());
        binding.rvEstadisticas.setAdapter(adapterMes);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (binding.rvEstadisticas.getAdapter() instanceof MesAdapter) {
            cargarDatosEstadisticas(); // Regresa de la vista por Mes a la vista por Año
            return true;
        }
        finish();
        return true;
    }
}