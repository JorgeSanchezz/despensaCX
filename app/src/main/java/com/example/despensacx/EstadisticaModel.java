package com.example.despensacx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EstadisticaModel {

    public static class AnnoModel {
        private String anio;
        private double totalAnio;
        private List<MesModel> meses = new ArrayList<>();

        public AnnoModel(String anio) {
            this.anio = anio;
        }

        public String getAnio() { return anio; }
        public double getTotalAnio() { return totalAnio; }
        public void setTotalAnio(double totalAnio) { this.totalAnio = totalAnio; }
        public List<MesModel> getMeses() { return meses; }
    }

    public static class MesModel {
        private String mesNombre;
        private String mesAnioClave; // e.g. "2026-08"
        private double totalMes;
        private Map<String, Double> gastosPorTienda;

        public MesModel(String mesNombre, String mesAnioClave, double totalMes, Map<String, Double> gastosPorTienda) {
            this.mesNombre = mesNombre;
            this.mesAnioClave = mesAnioClave;
            this.totalMes = totalMes;
            this.gastosPorTienda = gastosPorTienda;
        }

        public String getMesNombre() { return mesNombre; }
        public String getMesAnioClave() { return mesAnioClave; }
        public double getTotalMes() { return totalMes; }
        public Map<String, Double> getGastosPorTienda() { return gastosPorTienda; }
    }
}