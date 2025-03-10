package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;

import org.apache.commons.lang3.time.StopWatch;

public class RandomAisles extends Heuristica {
    public RandomAisles(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems,
            int waveSizeLB,
            int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    @Override
    public String getName() {
        return "RandomAisles"; // Nombre propio de la subclase
    }

    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        int[] pesosAisle = new int[as];

        for (int o = 0; o < os; ++o) {

            if (ordersh[o].size > waveSizeUB)
                continue;
            for (int p = 0; p < as; ++p) {
                int ocupa = 0;
                for (Map.Entry<Integer, Integer> entry : ordersh[o].items.entrySet()) {
                    ocupa += Math.min(aislesh[p].items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());
                }
                pesosAisle[p] += ocupa;
            }
        }

        Arrays.sort(ordersh, (o1, o2) -> Integer.compare(o2.size, o1.size));
        Arrays.sort(aislesh, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));
        Cart rtaH2= pasada(ordersh, aislesh, as);

        Arrays.sort(aislesh, (a1, a2) -> Integer.compare(a2.size, a1.size));

        rtaH2.update(pasada(ordersh, aislesh, as));

        int tope = rtaH2.getTope();

        Cart rta = new Cart();
        
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < as; i++)
            indices.add(i);

        for (int it = 0; it < 10000; it++) {
            Collections.shuffle(indices);
            int randomNum = ThreadLocalRandom.current().nextInt(1, tope + 1);

            Cart actual = new Cart();
            for(int i = 0; i <= randomNum; ++i) 
                actual.addAisle(indices.get(i));
            
            actual.fill();

            if (actual.cantItems >= waveSizeLB)
                rta.update(actual);

            tope = Math.min(tope, rta.getTope());

        }

        return new ChallengeSolution(rta.my_orders, rta.my_aisles);
    }
}