package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

public class BF5yH2 extends Heuristica {
    public BF5yH2(List<Map<Integer, Integer>> ordersh, List<Map<Integer, Integer>> aislesh, int nItems, int waveSizeLB,
            int waveSizeUB) {
        super(ordersh, aislesh, nItems, waveSizeLB, waveSizeUB);
    }

    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        int[] pesosAisle = new int[as];

        for (int o = 0; o < os; ++o) {
            if (orders[o].size > waveSizeUB)
                continue;
            for (int p = 0; p < as; ++p) {
                int ocupa = 0;
                for (Map.Entry<Integer, Integer> entry : orders[o].items.entrySet()) {
                    ocupa += Math.min(aisles[p].items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());
                }
                pesosAisle[p] += ocupa;
            }
        }

        Arrays.sort(orders, (o1, o2) -> Integer.compare(o2.size, o1.size));
        Arrays.sort(aisles, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));



        int tomo_pasillos = Math.min(10, as); // Conjunto {0,1,2,3,4,5,6,7,8,9}
        int totalSubsets = 1 << tomo_pasillos; // 2^n subconjuntos

        Cart rta = pasada(as);

        for (int mask = 1; mask < totalSubsets; mask++) {
            ArrayList<Integer> indices = new ArrayList<>();
            for (int i = 0; i < tomo_pasillos; i++)
                if ((mask & (1 << i)) != 0)
                    indices.add(i);

            int tope = as-1;
            if (rta.getValue() > 0)
                    tope = Math.min((int) Math.floor(waveSizeUB / rta.getValue()), as);
            for (int sol = tomo_pasillos - 1; sol <= tope; ++sol) {
                Cart actual = new Cart();

                for (int p : indices)
                    actual.addAisle(aisles[p]);

                for (int p = tomo_pasillos; p <= sol; ++p)
                    actual.addAisle(aisles[p]);

                actual.fill();

                actual.removeRedundantAisles();

                if (actual.cantItems >= waveSizeLB)
                    rta.update(actual);

                if (rta.getValue() > 0)
                    tope = Math.min((int) Math.floor(waveSizeUB / rta.getValue()), as);

            }
        }

        return new ChallengeSolution(rta.my_orders, rta.my_aisles);
    }
}