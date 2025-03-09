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

public class BF10yH2 extends Heuristica {
    public BF10yH2(List<Map<Integer, Integer>> ordersh, List<Map<Integer, Integer>> aislesh, int nItems, int waveSizeLB, int waveSizeUB) {
        super(ordersh, aislesh, nItems, waveSizeLB, waveSizeUB);
    }

    @Override
    public String getName() {
        return "BF10yH2";  // Nombre propio de la subclase
    }

    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
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
        }

        Arrays.sort(ordersh, (o1, o2) -> Integer.compare(o2.size, o1.size));
        Arrays.sort(aislesh, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));

        Set<Integer> rta_os = new HashSet<>();
        Set<Integer> rta_ps = new HashSet<>();
        double rta_val = 0;

        int tomo_pasillos = 5; // Conjunto {0,1,2,3,4,5,6,7,8,9}
        int totalSubsets = 1 << Math.min(tomo_pasillos, ps); // 2^n subconjuntos
        
        for (int mask = 0; mask < totalSubsets; mask++) {
            Cart actual = new Cart();
            for (int i = 0; i < tomo_pasillos; i++) 
                if ((mask & (1 << i)) != 0) actual.addAisle(i);
            
            for (int i = tomo_pasillos-1; i < ps; i++) {
                for (int j = tomo_pasillos; j < i+1; j++)
                    actual.addAisle(j);

                actual.nItems = 0;
                for(int o = 0; o < os; ++o) {
                    if(actual.nItems + ordersh[o].size <= waveSizeUB && tryFill(ordersh[o].items, actual.available)) {
                        actual.nItems += ordersh[o].size;
                        actual.my_orders.add(ordersh[o].id);
                    }
                }
                if(actual.nItems >= waveSizeLB && (double) actual.nItems / actual.my_aisles.size() > rta_val ) {
                    rta_val = (double)actual.nItems / actual.my_aisles.size();
                    rta_os = new HashSet<Integer>(actual.my_orders); 
                    rta_ps = new HashSet<Integer>(actual.my_aisles); 
                }
            }

        }

        return new ChallengeSolution(rta_os, rta_ps);
    }
}