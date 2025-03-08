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

public class BF10 extends ChallengeSolver {
    public BF10(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    @Override
    public String getName() {
        return "BF10";  // Nombre propio de la subclase
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
        Arrays.sort(indices_o, (i1, i2) -> Integer.compare(o_size[i2], o_size[i1]));
        
        Integer[] indices_p = new Integer[ps];
        for (int i = 0; i < ps; i++) indices_p[i] = i;
        Arrays.sort(indices_p, (i1, i2) -> Integer.compare(ps_copados[i2], ps_copados[i1]));


        int tomo_pasillos = Math.min(15, ps); // Conjunto {0,1,2,3,4,5,6,7,8,9}
        int totalSubsets = 1 << tomo_pasillos; // 2^n subconjuntos


        Set<Integer> rta_os = new HashSet<>();
        Set<Integer> rta_ps = new HashSet<>();
        double rta_val = 0;
        for (int mask = 0; mask < totalSubsets; mask++) {
            Map<Integer, Integer> mapa = new HashMap<>();
            Set<Integer> actual_os = new HashSet<>();
            Set<Integer> actual_ps = new HashSet<>();
            for (int i = 0; i < tomo_pasillos; i++) {
                if ((mask & (1 << i)) == 0) continue;
                actual_ps.add(indices_p[i]);
                for (Map.Entry<Integer, Integer> entry : aisles.get(indices_p[i]).entrySet()) {
                    int elem = entry.getKey(), cant = entry.getValue();
                    mapa.merge(elem, cant, Integer::sum);
                }  
            }


            Integer mirta = 0;
            for(int o = 0; o < os; ++o) {
                if(mirta + o_size[indices_o[o]] > waveSizeUB) continue;
                
                boolean anda = true;
                for (Map.Entry<Integer, Integer> entry : orders.get(indices_o[o]).entrySet()) {
                    int elem = entry.getKey(), cant = entry.getValue();
                    if(mapa.getOrDefault(elem, 0).intValue() < cant) anda = false;
                }
                if(!anda) continue;
                for (Map.Entry<Integer, Integer> entry : orders.get(indices_o[o]).entrySet()) {
                    int elem = entry.getKey(), cant = entry.getValue();
                    mapa.put(elem, mapa.get(elem) - cant);
                }
                mirta += o_size[indices_o[o]];
                actual_os.add(indices_o[o]);
            }
            if(mirta >= waveSizeLB && (double) mirta / actual_ps.size() > rta_val ) {
                rta_val = (double)mirta / actual_ps.size();
                rta_os = new HashSet<Integer>(actual_os); 
                rta_ps = new HashSet<Integer>(actual_ps); 
            }
        }

        return new ChallengeSolution(rta_os, rta_ps);
    }
}