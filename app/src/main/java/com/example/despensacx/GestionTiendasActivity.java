package com.example.despensacx;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.despensacx.databinding.ActivityGestionTiendasBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GestionTiendasActivity extends AppCompatActivity implements TiendaAdapter.OnTiendaActionListener {

    private ActivityGestionTiendasBinding binding;
    private AppDatabase db;
    private TiendaAdapter adapter;
    private String colorSeleccionadoHex = "#4CAF50"; // Por defecto

    private final String[] paletaColores = {
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800",
            "#FF5722", "#795548", "#9E9E9E", "#607D8B"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGestionTiendasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarTiendas);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = AppDatabase.getInstance(this);

        adapter = new TiendaAdapter(this);
        binding.rvTiendas.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTiendas.setAdapter(adapter);

        binding.fabNuevaTienda.setOnClickListener(v -> mostrarDialogoTienda(null));

        db.tiendaDao().getAllTiendas().observe(this, tiendas -> {
            adapter.setTiendas(tiendas);
        });
    }

    private void mostrarDialogoTienda(TiendaEntity tiendaEditar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_nueva_tienda, null);

        EditText etNombre = view.findViewById(R.id.etNombreTienda);
        GridLayout gridColor = view.findViewById(R.id.gridColorPicker);

        colorSeleccionadoHex = (tiendaEditar != null) ? tiendaEditar.getColor() : paletaColores[0];

        if (tiendaEditar != null) etNombre.setText(tiendaEditar.getNombre());

        // Generar Muestras Visuales de Colores
        for (String hex : paletaColores) {
            View colorCircle = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 100;
            params.height = 100;
            params.setMargins(12, 12, 12, 12);
            colorCircle.setLayoutParams(params);
            colorCircle.setBackgroundColor(Color.parseColor(hex));

            colorCircle.setOnClickListener(v -> {
                colorSeleccionadoHex = hex;
            });
            gridColor.addView(colorCircle);
        }

        builder.setView(view)
                .setTitle(tiendaEditar == null ? "Agregar Nueva Tienda" : "Editar Tienda")
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if (nombre.isEmpty()) return;

                    String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (tiendaEditar == null) {
                            TiendaEntity nueva = new TiendaEntity(nombre, colorSeleccionadoHex, fecha, 99);
                            db.tiendaDao().insert(nueva);
                        } else {
                            tiendaEditar.setNombre(nombre);
                            tiendaEditar.setColor(colorSeleccionadoHex);
                            db.tiendaDao().update(tiendaEditar);
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onEditar(TiendaEntity tienda) {
        mostrarDialogoTienda(tienda);
    }

    @Override
    public void onEliminar(TiendaEntity tienda) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = db.tiendaDao().countProductosByTiendaSync(tienda.getId());
            runOnUiThread(() -> {
                if (count > 0) {
                    new AlertDialog.Builder(this)
                            .setTitle("Acción Bloqueada")
                            .setMessage("No se puede eliminar la tienda porque tiene productos asociados en tus listas.")
                            .setPositiveButton("Aceptar", null)
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Eliminar Tienda")
                            .setMessage("¿Deseas eliminar '" + tienda.getNombre() + "'?")
                            .setPositiveButton("Eliminar", (d, w) -> {
                                AppDatabase.databaseWriteExecutor.execute(() -> db.tiendaDao().delete(tienda));
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                }
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}