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
import java.util.Collections;

import org.apache.commons.lang3.time.StopWatch;

public class H2yShuffleAisles extends Heuristica {
    public H2yShuffleAisles(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    @Override
    public String getName() {
        return "H2yShuffleAisles";  // Nombre propio de la subclase
    }
    
    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        int[] pesosAisle = new int[as];

        
        for(int o = 0; o < os; ++o) {          
            if(orders[o].size > waveSizeUB) continue;
            for(int p = 0; p < as; ++p) {
                int ocupa = 0;
                for (Map.Entry<Integer, Integer> entry : orders[o].items.entrySet()) {
                    ocupa += Math.min(aisles[p].items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());
                }
                pesosAisle[p] += ocupa;
            }   
        }


        Arrays.sort(orders, (o1, o2) -> Integer.compare(o2.size, o1.size));
        Arrays.sort(aisles, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));
        Cart rtaH2 = pasada(as);

        Arrays.sort(aisles, (a1, a2) -> Integer.compare(a2.size, a1.size));

        rtaH2.update(pasada(as));

        int tope = rtaH2.getTope();

        Cart rta = new Cart();
        for(int it = 0; it < 200; ++it) {
            List<Aisle> list = Arrays.asList(aisles);  // Convertir el array a lista
            Collections.shuffle(list);  // Shuffle la lista
            aisles = list.toArray(new Aisle[0]);

            rta.update(pasada(tope));
            tope = Math.min(tope, rta.getTope());
        }

        return new ChallengeSolution(rta.my_orders, rta.my_aisles);
    }
}