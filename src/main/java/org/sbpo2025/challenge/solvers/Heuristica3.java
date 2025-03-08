package org.sbpo2025.challenge.solvers;  // Paquete correcto

import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;

import org.apache.commons.lang3.time.StopWatch;

public class Heuristica3 extends ChallengeSolver {
    public Heuristica3(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    @Override
    public String getName() {
        return "Heuristica3";  // Nombre propio de la subclase
    }
    
    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        int os = orders.size();
        int ps = aisles.size();
        int[] ps_copados = new int[ps];
        int[] o_size = new int[os];
        
        for(int o = 0; o < os; ++o) {
            for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet()) 
                o_size[o] += entry.getValue();
            
            if(o_size[o] > waveSizeUB) continue;
            for(int p = 0; p < ps; ++p) {
                int ocupa = 0;
                for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet()) {
                    int cant = entry.getValue();
                    int elem = entry.getKey();
                    ocupa += Math.min(aisles.get(p).getOrDefault(elem, 0).intValue(), cant);
                }
                ps_copados[p] += ocupa;
                // if(ocupa > o_size[o] * 0.50) ps_copados[p]++;
                
            }   
        }

        Integer[] indices_o = new Integer[os];
        for (int i = 0; i < os; i++) indices_o[i] = i;
        Arrays.sort(indices_o, (i1, i2) -> Integer.compare(o_size[i1], o_size[i2]));
        
        Integer[] indices_p = new Integer[ps];
        for (int i = 0; i < ps; i++) indices_p[i] = i;
        Arrays.sort(indices_p, (i1, i2) -> Integer.compare(ps_copados[i2], ps_copados[i1]));

        Map<Integer, Integer> mapa_actual_ps = new HashMap<>();
        Set<Integer> rta_os = new HashSet<>();
        Set<Integer> rta_ps = new HashSet<>(), actual_ps = new HashSet<>();
        double rta_val = 0;
        for(int sol = 0; sol < ps; ++sol) {
            actual_ps.add(indices_p[sol]);
            for (Map.Entry<Integer, Integer> entry : aisles.get(indices_p[sol]).entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                mapa_actual_ps.merge(elem, cant, Integer::sum);
            }
            Map<Integer, Integer> copia_m = new HashMap<>(mapa_actual_ps);
            Integer mirta = 0;
            Set<Integer> actual_os = new HashSet<>();
            for(int o = 0; o < os; ++o) {
                if(mirta + o_size[indices_o[o]] > waveSizeUB) continue;
                
                boolean anda = true;
                for (Map.Entry<Integer, Integer> entry : orders.get(indices_o[o]).entrySet()) {
                    int elem = entry.getKey(), cant = entry.getValue();
                    if(copia_m.getOrDefault(elem, 0).intValue() < cant) anda = false;
                }
                if(!anda) continue;
                for (Map.Entry<Integer, Integer> entry : orders.get(indices_o[o]).entrySet()) {
                    int elem = entry.getKey(), cant = entry.getValue();
                    copia_m.put(elem, copia_m.get(elem) - cant);
                }
                mirta += o_size[indices_o[o]];
                actual_os.add(indices_o[o]);
            }
            if(mirta >= waveSizeLB && (double) mirta / (sol + 1) > rta_val ) {
                rta_val = (double)mirta / (sol + 1);
                rta_os = new HashSet<Integer>(actual_os); 
                rta_ps = new HashSet<Integer>(actual_ps); 
            }
        }

        return new ChallengeSolution(rta_os, rta_ps);
    }
}