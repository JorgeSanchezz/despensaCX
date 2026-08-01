package com.example.despensacx;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.despensacx.databinding.ActivityDetalleListaBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetalleListaActivity extends AppCompatActivity implements ProductoAdapter.OnProductoActionListener {

    private ActivityDetalleListaBinding binding;
    private AppDatabase db;
    private long listaId;
    private ListaEntity listaActual;
    private ProductoAdapter adapter;
    private List<TiendaEntity> catalogoTiendas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetalleListaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarDetalle);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        listaId = getIntent().getLongExtra("EXTRA_LISTA_ID", -1);
        if (listaId == -1) {
            finish();
            return;
        }

        db = AppDatabase.getInstance(this);

        adapter = new ProductoAdapter(this);
        binding.rvProductos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProductos.setAdapter(adapter);

        binding.fabAgregarProducto.setOnClickListener(v -> mostrarDialogoProducto(null));

        binding.cbSeleccionarTodos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.productoDao().setAllSeleccionadoSync(listaId, isChecked);
                actualizarFechaModificacion();
            });
        });

        binding.btnCompartirLista.setOnClickListener(v -> compartirLista());

        // Cargar Lista y Tiendas
        db.listaDao().getById(listaId).observe(this, lista -> {
            if (lista != null) {
                listaActual = lista;
                actualizarEncabezado();
            }
        });

        db.tiendaDao().getAllTiendas().observe(this, tiendas -> {
            catalogoTiendas = tiendas;
            cargarProductosYAgrupar();
        });
    }

    private void cargarProductosYAgrupar() {
        db.productoDao().getProductosByLista(listaId).observe(this, productos -> {
            Map<Long, List<ProductoConTienda>> agrupado = new HashMap<>();
            double totalGeneral = 0.0;

            for (ProductoEntity p : productos) {
                TiendaEntity tEncontrada = null;
                for (TiendaEntity t : catalogoTiendas) {
                    if (t.getId() == p.getTiendaId()) {
                        tEncontrada = t;
                        break;
                    }
                }
                if (tEncontrada == null) {
                    tEncontrada = new TiendaEntity("General", "#9E9E9E", "", 99);
                }

                ProductoConTienda pct = new ProductoConTienda(p, tEncontrada);
                if (!agrupado.containsKey(pct.getTienda().getId())) {
                    agrupado.put(pct.getTienda().getId(), new ArrayList<>());
                }
                agrupado.get(pct.getTienda().getId()).add(pct);

                if (p.isSeleccionado()) {
                    totalGeneral += (p.getPrecio() * p.getCantidad());
                }
            }

            // Construir Lista plana para Adapter con Headers
            List<ProductoAdapter.DisplayItem> itemsDisplay = new ArrayList<>();
            for (Map.Entry<Long, List<ProductoConTienda>> entry : agrupado.entrySet()) {
                List<ProductoConTienda> listaProds = entry.getValue();
                double subtotalTienda = 0.0;
                for (ProductoConTienda pct : listaProds) {
                    if (pct.getProducto().isSeleccionado()) {
                        subtotalTienda += (pct.getProducto().getPrecio() * pct.getProducto().getCantidad());
                    }
                }

                String nombreT = listaProds.get(0).getTienda().getNombre();
                String colorT = listaProds.get(0).getTienda().getColor();
                itemsDisplay.add(new ProductoAdapter.DisplayItem(nombreT, colorT, subtotalTienda));

                for (ProductoConTienda pct : listaProds) {
                    itemsDisplay.add(new ProductoAdapter.DisplayItem(pct));
                }
            }

            adapter.setItems(itemsDisplay);
            binding.tvGranTotal.setText(String.format(Locale.getDefault(), "$%.2f MXN", totalGeneral));
            actualizarBarraPresupuesto(totalGeneral);
        });
    }

    private void actualizarEncabezado() {
        String titulo = listaActual.getNombre() + (listaActual.isArchivada() ? " (Archivada)" : "");
        binding.tvNombreDetalle.setText(titulo);
        binding.tvModificadoDetalle.setText("Modificado: " + listaActual.getFechaModificacion());
    }

    private void actualizarBarraPresupuesto(double totalAcumulado) {
        if (listaActual == null) return;
        double max = listaActual.getPresupuestoMaximo();

        if (max > 0) {
            binding.tvTextoPresupuesto.setText(String.format(Locale.getDefault(), "$%.2f / $%.2f MXN", totalAcumulado, max));
            int porcentaje = (int) ((totalAcumulado / max) * 100);
            binding.progressPresupuesto.setProgress(Math.min(porcentaje, 100));

            if (totalAcumulado > max) {
                binding.tvEstadoPresupuesto.setText("¡EXCEDIDO!");
                binding.tvEstadoPresupuesto.setTextColor(Color.RED);
                binding.progressPresupuesto.setIndicatorColor(Color.RED);
            } else {
                binding.tvEstadoPresupuesto.setText("DENTRO DEL LÍMITE");
                binding.tvEstadoPresupuesto.setTextColor(Color.parseColor("#2E7D32"));
                binding.progressPresupuesto.setIndicatorColor(Color.parseColor("#2E7D32"));
            }
        } else {
            binding.tvTextoPresupuesto.setText(String.format(Locale.getDefault(), "$%.2f MXN", totalAcumulado));
            binding.tvEstadoPresupuesto.setText("SIN LÍMITE");
            binding.progressPresupuesto.setProgress(100);
            binding.progressPresupuesto.setIndicatorColor(Color.GRAY);
        }
    }

    private void mostrarDialogoProducto(ProductoEntity productoEditar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_nuevo_producto, null);

        EditText etDesc = view.findViewById(R.id.etDescripcionProducto);
        EditText etPrecio = view.findViewById(R.id.etPrecioProducto);
        EditText etCantidad = view.findViewById(R.id.etCantidadProducto);
        Spinner spinner = view.findViewById(R.id.spinnerTiendasProducto);

        List<String> nombresTiendas = new ArrayList<>();
        nombresTiendas.add("Seleccionar tienda");
        for (TiendaEntity t : catalogoTiendas) {
            nombresTiendas.add(t.getNombre());
        }

        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nombresTiendas);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapterSpinner);

        if (productoEditar != null) {
            etDesc.setText(productoEditar.getDescripcion());
            etPrecio.setText(String.valueOf(productoEditar.getPrecio()));
            etCantidad.setText(String.valueOf(productoEditar.getCantidad()));
            for (int i = 0; i < catalogoTiendas.size(); i++) {
                if (catalogoTiendas.get(i).getId() == productoEditar.getTiendaId()) {
                    spinner.setSelection(i + 1);
                    break;
                }
            }
        }

        builder.setView(view)
                .setTitle(productoEditar == null ? "Agregar Producto" : "Editar Producto")
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String desc = etDesc.getText().toString().trim();
                    String precioStr = etPrecio.getText().toString().trim();
                    String cantStr = etCantidad.getText().toString().trim();
                    int posTienda = spinner.getSelectedItemPosition();

                    if (desc.isEmpty()) {
                        Toast.makeText(this, "Descripción requerida", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (posTienda == 0) {
                        Toast.makeText(this, "Debes seleccionar una tienda válida", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double precio = precioStr.isEmpty() ? 0.0 : Double.parseDouble(precioStr);
                    int cantidad = cantStr.isEmpty() ? 1 : Integer.parseInt(cantStr);
                    long tiendaIdSelected = catalogoTiendas.get(posTienda - 1).getId();

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (productoEditar == null) {
                            ProductoEntity p = new ProductoEntity(listaId, tiendaIdSelected, desc, precio, cantidad, true);
                            db.productoDao().insert(p);
                        } else {
                            productoEditar.setDescripcion(desc);
                            productoEditar.setPrecio(precio);
                            productoEditar.setCantidad(cantidad);
                            productoEditar.setTiendaId(tiendaIdSelected);
                            db.productoDao().update(productoEditar);
                        }
                        actualizarFechaModificacion();
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void actualizarFechaModificacion() {
        if (listaActual != null) {
            String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            listaActual.setFechaModificacion(fecha);
            db.listaDao().update(listaActual);
        }
    }

    private void compartirLista() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ProductoEntity> prods = db.productoDao().getProductosByListaSync(listaId);
            StringBuilder sb = new StringBuilder();
            sb.append("🛒 *").append(listaActual.getNombre()).append("*\n\n");

            Map<Long, List<ProductoEntity>> mapa = new HashMap<>();
            for (ProductoEntity p : prods) {
                if (!mapa.containsKey(p.getTiendaId())) mapa.put(p.getTiendaId(), new ArrayList<>());
                mapa.get(p.getTiendaId()).add(p);
            }

            for (TiendaEntity t : catalogoTiendas) {
                if (mapa.containsKey(t.getId())) {
                    sb.append("📍 *").append(t.getNombre()).append("*\n");
                    for (ProductoEntity p : mapa.get(t.getId())) {
                        String check = p.isSeleccionado() ? "[x]" : "[ ]";
                        sb.append(check).append(" ").append(p.getCantidad()).append("x ").append(p.getDescripcion()).append("\n");
                    }
                    sb.append("\n");
                }
            }

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Compartir Lista"));
        });
    }

    @Override
    public void onToggleSeleccion(ProductoEntity producto, boolean seleccionado) {
        producto.setSeleccionado(seleccionado);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.productoDao().update(producto);
            actualizarFechaModificacion();
        });
    }

    @Override
    public void onEditar(ProductoEntity producto) {
        mostrarDialogoProducto(producto);
    }

    @Override
    public void onEliminar(ProductoEntity producto) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.productoDao().delete(producto);
            actualizarFechaModificacion();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}