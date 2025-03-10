package org.sbpo2025.challenge.solvers; // Paquete correcto

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

public class BF10 extends Heuristica {
    public BF10(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB,
            int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    @Override
    public String getName() {
        return "BF10"; // Nombre propio de la subclase
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

        Cart rta = new Cart();

        int tomo_pasillos = Math.min(10, as); // Conjunto {0,1,2,3,4,5,6,7,8,9}
        int totalSubsets = 1 << tomo_pasillos; // 2^n subconjuntos

        for (int mask = 0; mask < totalSubsets; mask++) {
            Cart actual = new Cart();
            for (int i = 0; i < tomo_pasillos; i++)
                if ((mask & (1 << i)) != 0)
                    actual.addAisle(i);

            actual.fill();
            
            if (actual.cantItems >= waveSizeLB)
                rta.update(actual);

        }

        return new ChallengeSolution(rta.my_orders, rta.my_aisles);
    }
}