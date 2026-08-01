package com.example.despensacx;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.despensacx.databinding.ActivityEstadisticasBinding;
import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EstadisticasActivity extends AppCompatActivity {

    private ActivityEstadisticasBinding binding;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEstadisticasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarEstadisticas);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = AppDatabase.getInstance(this);

        calcularEstadisticas();
    }

    private void calcularEstadisticas() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ProductoEntity> productos = db.productoDao().getAllSync();
            List<TiendaEntity> tiendas = db.tiendaDao().getAllTiendasSync();

            Map<Long, Double> gastoPorTienda = new HashMap<>();
            double totalGlobal = 0.0;

            for (ProductoEntity p : productos) {
                if (p.isSeleccionado()) {
                    double imp = p.getPrecio() * p.getCantidad();
                    totalGlobal += imp;
                    gastoPorTienda.put(p.getTiendaId(), gastoPorTienda.getOrDefault(p.getTiendaId(), 0.0) + imp);
                }
            }

            final double finalTotal = totalGlobal;

            runOnUiThread(() -> {
                binding.tvGastoTotalGlobal.setText(String.format(Locale.getDefault(), "$%.2f MXN", finalTotal));
                binding.containerEstadisticasTiendas.removeAllViews();

                for (TiendaEntity t : tiendas) {
                    double gastoTienda = gastoPorTienda.getOrDefault(t.getId(), 0.0);
                    double pct = finalTotal > 0 ? (gastoTienda / finalTotal) * 100 : 0.0;

                    MaterialCardView card = new MaterialCardView(this);
                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    cardParams.setMargins(0, 8, 0, 8);
                    card.setLayoutParams(cardParams);
                    card.setRadius(8f);

                    LinearLayout ll = new LinearLayout(this);
                    ll.setOrientation(LinearLayout.VERTICAL);
                    ll.setPadding(24, 24, 24, 24);

                    TextView tvTitle = new TextView(this);
                    tvTitle.setText(t.getNombre());
                    tvTitle.setTextSize(16f);
                    tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

                    TextView tvDetalle = new TextView(this);
                    tvDetalle.setText(String.format(Locale.getDefault(), "$%.2f MXN (%.1f%%)", gastoTienda, pct));
                    tvDetalle.setTextSize(14f);
                    tvDetalle.setTextColor(Color.parseColor("#555555"));

                    ll.addView(tvTitle);
                    ll.addView(tvDetalle);
                    card.addView(ll);

                    binding.containerEstadisticasTiendas.addView(card);
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