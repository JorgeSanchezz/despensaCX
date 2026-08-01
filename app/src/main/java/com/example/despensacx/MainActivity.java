package com.example.despensacx;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.despensacx.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ListaAdapter.OnListaClickListener {

    private ActivityMainBinding binding;
    private AppDatabase db;
    private ListaAdapter adapter;
    private List<ListaEntity> listaOriginal = new ArrayList<>();
    private int ordenActual = 0; // 0: Fecha, 1: Alfabetico, 2: Presupuesto

    // Lanzador para Exportación (SAF Create Document)
    private final ActivityResultLauncher<Intent> exportarJsonLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            boolean ok = BackupHelper.exportarJSONToUri(this, uri);
                            runOnUiThread(() ->
                                    Toast.makeText(this, ok ? "Respaldo guardado correctamente" : "Error al guardar respaldo", Toast.LENGTH_SHORT).show()
                            );
                        });
                    }
                }
            });

    // Lanzador para Importación (SAF Open Document)
    private final ActivityResultLauncher<Intent> importarJsonLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            boolean ok = BackupHelper.importarJSONFromUri(this, uri);
                            runOnUiThread(() ->
                                    Toast.makeText(this, ok ? "Respaldo restaurado con éxito" : "Error al leer respaldo", Toast.LENGTH_SHORT).show()
                            );
                        });
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarMain);

        db = AppDatabase.getInstance(this);

        setupRecyclerView();
        setupSearchView();

        binding.fabNuevaLista.setOnClickListener(v -> mostrarDialogoCrearLista(null));

        // Cargar listas activas desde Room
        db.listaDao().getListasActivas().observe(this, listas -> {
            listaOriginal = listas;
            aplicarFiltroYOrden(binding.searchViewListas.getQuery().toString());
        });
    }

    private void setupRecyclerView() {
        adapter = new ListaAdapter(this);
        binding.rvListas.setLayoutManager(new LinearLayoutManager(this));
        binding.rvListas.setAdapter(adapter);

        // Gestos Swipe (Derecha: Archivar, Izquierda: Eliminar)
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ListaEntity lista = adapter.getListaAt(position);

                if (direction == ItemTouchHelper.RIGHT) {
                    // Archivar
                    archivarLista(lista, true);
                    Snackbar.make(binding.getRoot(), "Lista archivada", Snackbar.LENGTH_LONG)
                            .setAction("DESHACER", v -> archivarLista(lista, false))
                            .show();
                } else {
                    // Eliminar
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Eliminar lista")
                            .setMessage("¿Deseas eliminar la lista '" + lista.getNombre() + "'?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                AppDatabase.databaseWriteExecutor.execute(() -> db.listaDao().delete(lista));
                            })
                            .setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyItemChanged(position))
                            .show();
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(binding.rvListas);
    }

    private void setupSearchView() {
        binding.searchViewListas.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                aplicarFiltroYOrden(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                aplicarFiltroYOrden(newText);
                return true;
            }
        });
    }

    private void aplicarFiltroYOrden(String query) {
        List<ListaEntity> filtradas = new ArrayList<>();
        for (ListaEntity l : listaOriginal) {
            if (l.getNombre().toLowerCase().contains(query.toLowerCase()) ||
                    l.getFechaCreacion().contains(query)) {
                filtradas.add(l);
            }
        }

        if (ordenActual == 0) {
            Collections.sort(filtradas, (a, b) -> b.getFechaCreacion().compareTo(a.getFechaCreacion()));
        } else if (ordenActual == 1) {
            Collections.sort(filtradas, (a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()));
        } else if (ordenActual == 2) {
            Collections.sort(filtradas, (a, b) -> Double.compare(b.getPresupuestoMaximo(), a.getPresupuestoMaximo()));
        }

        adapter.setListas(filtradas);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_tiendas) {
            startActivity(new Intent(this, GestionTiendasActivity.class));
            return true;
        } else if (id == R.id.action_archivados) {
            startActivity(new Intent(this, ListasArchivadasActivity.class));
            return true;
        } else if (id == R.id.action_estadisticas) {
            startActivity(new Intent(this, EstadisticasActivity.class));
            return true;
        } else if (id == R.id.action_ordenar) {
            mostrarDialogoOrden();
            return true;
        } else if (id == R.id.action_respaldo) {
            gestionarRespaldo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void mostrarDialogoOrden() {
        String[] opciones = {"Fecha de Creación", "Orden Alfabético", "Presupuesto Total"};
        new AlertDialog.Builder(this)
                .setTitle("Ordenar Listas Por")
                .setSingleChoiceItems(opciones, ordenActual, (dialog, which) -> {
                    ordenActual = which;
                    aplicarFiltroYOrden(binding.searchViewListas.getQuery().toString());
                    dialog.dismiss();
                })
                .show();
    }

    private void gestionarRespaldo() {
        String[] opciones = {"Exportar Respaldo (JSON)", "Importar Respaldo (JSON)"};
        new AlertDialog.Builder(this)
                .setTitle("Respaldo y Restauración")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        abrirExportadorSAF();
                    } else {
                        abrirImportadorSAF();
                    }
                })
                .show();
    }

    private void abrirExportadorSAF() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, BackupHelper.generarNombreDefectoRespaldo());
        exportarJsonLauncher.launch(intent);
    }

    private void abrirImportadorSAF() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        importarJsonLauncher.launch(intent);
    }

    private void mostrarDialogoCrearLista(ListaEntity listaEditar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_nueva_lista, null);
        EditText etNombre = view.findViewById(R.id.etNombreLista);
        EditText etPresupuesto = view.findViewById(R.id.etPresupuestoLista);

        if (listaEditar != null) {
            etNombre.setText(listaEditar.getNombre());
            if (listaEditar.getPresupuestoMaximo() > 0) {
                etPresupuesto.setText(String.valueOf(listaEditar.getPresupuestoMaximo()));
            }
        }

        builder.setView(view)
                .setTitle(listaEditar == null ? "Nueva Lista de Compras" : "Editar Lista")
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nombre = etNombre.getText().toString().trim();
                    String presupuestoStr = etPresupuesto.getText().toString().trim();
                    double presupuesto = presupuestoStr.isEmpty() ? 0.0 : Double.parseDouble(presupuestoStr);

                    if (nombre.isEmpty()) {
                        Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String fechaHoraActual = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (listaEditar == null) {
                            ListaEntity nueva = new ListaEntity(nombre, fechaHoraActual, fechaHoraActual, false, presupuesto);
                            db.listaDao().insert(nueva);
                        } else {
                            listaEditar.setNombre(nombre);
                            listaEditar.setPresupuestoMaximo(presupuesto);
                            listaEditar.setFechaModificacion(fechaHoraActual);
                            db.listaDao().update(listaEditar);
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void archivarLista(ListaEntity lista, boolean archivar) {
        String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        lista.setArchivada(archivar);
        lista.setFechaModificacion(fecha);
        AppDatabase.databaseWriteExecutor.execute(() -> db.listaDao().update(lista));
    }

    @Override
    public void onClick(ListaEntity lista) {
        Intent intent = new Intent(this, DetalleListaActivity.class);
        intent.putExtra("EXTRA_LISTA_ID", lista.getId());
        startActivity(intent);
    }

    @Override
    public void onLongClick(ListaEntity lista, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("Archivar Lista");
        popup.getMenu().add("Duplicar Lista");
        popup.getMenu().add("Eliminar Lista");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Archivar Lista")) {
                archivarLista(lista, true);
            } else if (item.getTitle().equals("Duplicar Lista")) {
                onDuplicar(lista);
            } else if (item.getTitle().equals("Eliminar Lista")) {
                onEliminar(lista);
            }
            return true;
        });
        popup.show();
    }

    @Override
    public void onEditar(ListaEntity lista) {
        mostrarDialogoCrearLista(lista);
    }

    @Override
    public void onDuplicar(ListaEntity lista) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            ListaEntity copia = new ListaEntity(lista.getNombre() + " (Copia)", fecha, fecha, false, lista.getPresupuestoMaximo());
            long nuevaId = db.listaDao().insert(copia);

            List<ProductoEntity> productos = db.productoDao().getProductosByListaSync(lista.getId());
            for (ProductoEntity p : productos) {
                ProductoEntity pCopia = new ProductoEntity(nuevaId, p.getTiendaId(), p.getDescripcion(), p.getPrecio(), p.getCantidad(), p.isSeleccionado());
                db.productoDao().insert(pCopia);
            }
        });
    }

    @Override
    public void onEliminar(ListaEntity lista) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar lista")
                .setMessage("¿Estás seguro de eliminar esta lista?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> db.listaDao().delete(lista));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}