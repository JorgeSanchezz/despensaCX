package com.example.despensacx;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class MesAdapter extends RecyclerView.Adapter<MesAdapter.MesViewHolder> {

    private final List<EstadisticaModel.MesModel> listaMeses;

    public MesAdapter(List<EstadisticaModel.MesModel> listaMeses) {
        this.listaMeses = listaMeses;
    }

    @NonNull
    @Override
    public MesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_estadistica_mes, parent, false);
        return new MesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MesViewHolder holder, int position) {
        EstadisticaModel.MesModel item = listaMeses.get(position);
        holder.tvMesNombre.setText(item.getMesNombre());
        holder.tvTotalMes.setText(FormatUtils.formatCurrency(item.getTotalMes()));

        // Desglose por Tiendas
        holder.containerTiendas.removeAllViews();
        Map<String, Double> gastosTienda = item.getGastosPorTienda();

        if (gastosTienda != null && !gastosTienda.isEmpty()) {
            for (Map.Entry<String, Double> entry : gastosTienda.entrySet()) {
                View row = LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.item_desglose_tienda, holder.containerTiendas, false);

                TextView tvNombreTienda = row.findViewById(R.id.tvNombreTienda);
                TextView tvMontoTienda = row.findViewById(R.id.tvMontoTienda);

                tvNombreTienda.setText(entry.getKey());
                tvMontoTienda.setText(FormatUtils.formatCurrency(entry.getValue()));

                holder.containerTiendas.addView(row);
            }
        }
    }

    @Override
    public int getItemCount() {
        return listaMeses != null ? listaMeses.size() : 0;
    }

    static class MesViewHolder extends RecyclerView.ViewHolder {
        TextView tvMesNombre, tvTotalMes;
        LinearLayout containerTiendas;

        public MesViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMesNombre = itemView.findViewById(R.id.tvMesNombre);
            tvTotalMes = itemView.findViewById(R.id.tvTotalMes);
            containerTiendas = itemView.findViewById(R.id.containerTiendas);
        }
    }
}