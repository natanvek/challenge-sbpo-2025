package org.sbpo2025.challenge.solvers;  // Paquete correcto

import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;

import org.apache.commons.lang3.time.StopWatch;

public class Heuristica2 extends Heuristica {
    public Heuristica2(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    @Override
    public String getName() {
        return "Heuristica2";  // Nombre propio de la subclase
    }
    
    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        System.out.println("waveSizeUB2: " + waveSizeUB);
        int os = ordersh.length;
        int ps = aislesh.length;
        int[] pesosAisle = new int[ps];

        
        for(int o = 0; o < os; ++o) {          
            if(ordersh[o].size > waveSizeUB) continue;
            for(int p = 0; p < ps; ++p) {
                int ocupa = 0;
                for (Map.Entry<Integer, Integer> entry : ordersh[o].items.entrySet()) {
                    ocupa += Math.min(aislesh[p].items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());
                }
                pesosAisle[p] += ocupa;
            }   
            System.out.println(pesosAisle);
        }


        Arrays.sort(ordersh, (o1, o2) -> Integer.compare(o2.size, o1.size));
        Arrays.sort(aislesh, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));


        Map<Integer, Integer> mapa_actual_ps = new HashMap<>();
        Set<Integer> rta_os = new HashSet<>();
        Set<Integer> rta_ps = new HashSet<>(), actual_ps = new HashSet<>();
        double rta_val = 0;
        for(int sol = 0; sol < ps; ++sol) {
            actual_ps.add(aislesh[sol].id);
            for (Map.Entry<Integer, Integer> entry : aislesh[sol].items.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                mapa_actual_ps.merge(elem, cant, Integer::sum);
            }
            Map<Integer, Integer> copia_m = new HashMap<>(mapa_actual_ps);
            Integer mirta = 0;
            Set<Integer> actual_os = new HashSet<>();
            for(int o = 0; o < os; ++o) {
                if(mirta + ordersh[o].size <= waveSizeUB && tryFill(ordersh[o].items, copia_m)) {
                    mirta += ordersh[o].size;
                    actual_os.add(ordersh[o].id);
                }
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