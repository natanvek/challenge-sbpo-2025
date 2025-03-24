package org.sbpo2025.challenge.solvers;  // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

public class Heuristica2 extends Heuristica {
    public Heuristica2(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }
    
    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        int[] pesosAisle = new int[as];

        
        for(Order o : orders) {          
            if(o.size > waveSizeUB) continue;
            for(Aisle p : aisles) {
                int ocupa = 0;

                for (Map.Entry<Integer, Integer> entry : o.items.entrySet()) 
                    ocupa += Math.min(p.items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());
                
                pesosAisle[p.id] += ocupa;
            }   
        }


        Arrays.sort(orders, (o1, o2) -> Integer.compare(o2.size, o1.size));
        Arrays.sort(aisles, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));
        EfficientCart rta = pasada(as);


        Arrays.sort(aisles, (a1, a2) -> Integer.compare(a2.size, a1.size));

        rta.update(pasada(as));

        return getSolution(rta);
    }
}