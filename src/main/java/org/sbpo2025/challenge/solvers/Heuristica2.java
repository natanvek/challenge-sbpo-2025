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
        int[] pesosAisle = new int[as];

        
        for(int o = 0; o < os; ++o) {          
            if(ordersh[o].size > waveSizeUB) continue;
            for(int p = 0; p < as; ++p) {
                int ocupa = 0;
                for (Map.Entry<Integer, Integer> entry : ordersh[o].items.entrySet()) {
                    ocupa += Math.min(aislesh[p].items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());
                }
                pesosAisle[p] += ocupa;
            }   
        }


        Arrays.sort(ordersh, (o1, o2) -> Integer.compare(o2.size, o1.size));
        Arrays.sort(aislesh, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));
        Cart rta = pasada(ordersh, aislesh, as);

        Arrays.sort(aislesh, (a1, a2) -> Integer.compare(a2.size, a1.size));

        rta.update(pasada(ordersh, aislesh, as));

        return new ChallengeSolution(rta.my_orders, rta.my_aisles);
    }
}