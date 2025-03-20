package org.sbpo2025.challenge.solvers;  // Paquete correcto

import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;

import org.apache.commons.lang3.time.StopWatch;

public class Heuristica2b extends Heuristica {
    public Heuristica2b(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    
    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        int os = orders.length;
        int ps = aisles.length;
        int[] pesosAisle = new int[ps];
        boolean[] vistosAisle = new boolean[ps];
        for (int i = 0; i < ps; ++i) {
            vistosAisle[i] = false;
        }

        for(int o = 0; o < os; ++o) {          
            if(orders[o].size > waveSizeUB) continue;
            for(int p = 0; p < ps; ++p) {
                int ocupa = 0;
                for (Map.Entry<Integer, Integer> entry : orders[o].items.entrySet()) {
                    ocupa += Math.min(aisles[p].items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());
                }
                pesosAisle[p] += ocupa;
            }   
        }


        // Arrays.sort(orders, (o1, o2) -> Integer.compare(o2.size, o1.size));
        Arrays.sort(aisles, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));


        Map<Integer, Integer> mapa_actual_ps = new HashMap<>();
        Set<Integer> rta_os = new HashSet<>();
        Set<Integer> rta_ps = new HashSet<>(), actual_ps = new HashSet<>();
        double rta_val = 0;
        for(int checked = 0; checked < ps; ++checked) {
            // max pasillo
            int current = -1;
            for (int i = 0; i < ps; ++i) {
                if (vistosAisle[i]) {
                    continue;
                }
                if (current == -1 || pesosAisle[current] < pesosAisle[i]) {
                    current = i;
                }
            }
            if (current == -1) {
                break;
            }
            vistosAisle[current] = true;
            actual_ps.add(aisles[current].id);
            for (Map.Entry<Integer, Integer> entry : aisles[current].items.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                mapa_actual_ps.merge(elem, cant, Integer::sum);
            }
            Map<Integer, Integer> copia_m = new HashMap<>(mapa_actual_ps);
            Integer mirta = 0;
            Set<Integer> actual_os = new HashSet<>();
            // for(int o = 0; o < os; ++o) {
            //     if(mirta + orders[o].size <= waveSizeUB && tryFill(orders[o].items, copia_m)) {
            //         mirta += orders[o].size;
            //         actual_os.add(orders[o].id);
            //     }
            // }
            if(mirta >= waveSizeLB && (double) mirta / (checked + 1) > rta_val ) {
                rta_val = (double)mirta / (checked + 1);
                rta_os = new HashSet<Integer>(actual_os); 
                rta_ps = new HashSet<Integer>(actual_ps); 
            }
            // recalculo pesos
            for(int p = 0; p < ps; ++p) {
                pesosAisle[p] = 0;
            }
            for(int o = 0; o < os; ++o) {          
                if(orders[o].size > waveSizeUB) continue;
                for(int p = 0; p < ps; ++p) {
                    if (vistosAisle[p]) {
                        continue;
                    }
                    int ocupa = 0;
                    for (Map.Entry<Integer, Integer> entry : orders[o].items.entrySet()) {
                        ocupa += Math.min(copia_m.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());
                    }
                    pesosAisle[p] += ocupa;
                }   
            }
        }
        return new ChallengeSolution(rta_os, rta_ps);
    }
}