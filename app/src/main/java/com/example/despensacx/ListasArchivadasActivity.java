package com.example.despensacx;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.despensacx.databinding.ActivityListasArchivadasBinding;

import java.util.ArrayList;
import java.util.List;

public class ListasArchivadasActivity extends AppCompatActivity implements ListaAdapter.OnListaClickListener {

    private ActivityListasArchivadasBinding binding;
    private AppDatabase db;
    private ListaAdapter adapter;
    private List<ListaEntity> listaArchivadasOriginal = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListasArchivadasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarArchivadas);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = AppDatabase.getInstance(this);

        adapter = new ListaAdapter(this);
        binding.rvListasArchivadas.setLayoutManager(new LinearLayoutManager(this));
        binding.rvListasArchivadas.setAdapter(adapter);

        db.listaDao().getListasArchivadas().observe(this, listas -> {
            listaArchivadasOriginal = listas;
            filtrar(binding.searchViewArchivadas.getQuery().toString());
        });

        binding.searchViewArchivadas.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filtrar(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filtrar(newText);
                return true;
            }
        });
    }

    private void filtrar(String query) {
        List<ListaEntity> res = new ArrayList<>();
        for (ListaEntity l : listaArchivadasOriginal) {
            if (l.getNombre().toLowerCase().contains(query.toLowerCase())) {
                res.add(l);
            }
        }
        adapter.setListas(res);
    }

    @Override
    public void onClick(ListaEntity lista) {
        Intent intent = new Intent(this, DetalleListaActivity.class);
        intent.putExtra("EXTRA_LISTA_ID", lista.getId());
        startActivity(intent);
    }

    @Override
    public void onLongClick(ListaEntity lista, android.view.View view) {
        new AlertDialog.Builder(this)
                .setTitle("Opciones de Archivada")
                .setItems(new String[]{"Desarchivar Lista", "Eliminar definitivamente"}, (dialog, which) -> {
                    if (which == 0) {
                        lista.setArchivada(false);
                        AppDatabase.databaseWriteExecutor.execute(() -> db.listaDao().update(lista));
                    } else {
                        onEliminar(lista);
                    }
                })
                .show();
    }

    @Override
    public void onEditar(ListaEntity lista) { }

    @Override
    public void onDuplicar(ListaEntity lista) { }

    @Override
    public void onEliminar(ListaEntity lista) {
        AppDatabase.databaseWriteExecutor.execute(() -> db.listaDao().delete(lista));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}