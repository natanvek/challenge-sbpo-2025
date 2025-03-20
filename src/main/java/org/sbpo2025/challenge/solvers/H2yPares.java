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

public class H2yPares extends Heuristica {
    public H2yPares(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        double[][] OP = new double[os][as];
        int[] Oq = new int[os];
        double[] Av = new double[as];

        for (int o = 0; o < os; ++o) {
            Oq[o] = 0;
            for (Map.Entry<Integer, Integer> entry : orders[o].items.entrySet()) {
                Oq[o] += entry.getValue();
            }
        }
        for(int o = 0; o < os; ++o) {          
            if(orders[o].size > waveSizeUB) continue;
            for(int p = 0; p < as; ++p) {
                int value = 0;
                for (Map.Entry<Integer, Integer> entry : orders[o].items.entrySet()) {
                    value += Math.min(aisles[p].items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());
                }
                OP[o][p] = (double) value / Oq[o];
            }
        }
        for(int p = 0; p < as; ++p) {
            Av[p] = 0;
            for (int o = 0; o < os; ++o) {
                Av[p] += OP[o][p];
            }
        }
        Arrays.sort(aisles, (a1, a2) -> Double.compare(Av[a2.id], Av[a1.id]));

        Map<Integer, Integer> mapa_actual_ps = new HashMap<>();
        Set<Integer> rta_os = new HashSet<>();
        Set<Integer> rta_ps = new HashSet<>(), actual_ps = new HashSet<>();
        double rta_val = 0;
        for(int p = 0; p < as; ++p) {
            actual_ps.add(aisles[p].id);
            for (Map.Entry<Integer, Integer> entry : aisles[p].items.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                mapa_actual_ps.merge(elem, cant, Integer::sum);
            }
            Map<Integer, Integer> copia_m = new HashMap<>(mapa_actual_ps);
            Integer mirta = 0;
            Set<Integer> actual_os = new HashSet<>();
            final int p2 = p;
            Arrays.sort(orders, (o1, o2) -> Double.compare(OP[o2.id][p2], OP[o1.id][p2]));
            for(Order order : orders) {
                // if(mirta + order.size <= waveSizeUB && tryFill(order.items, copia_m)) {
                //     mirta += order.size;
                //     actual_os.add(order.id);
                // }
            }
            if(mirta >= waveSizeLB && (double) mirta / (p + 1) > rta_val ) {
                rta_val = (double)mirta / (p + 1);
                rta_os = new HashSet<Integer>(actual_os); 
                rta_ps = new HashSet<Integer>(actual_ps); 
            }
        }

        return new ChallengeSolution(rta_os, rta_ps);
    }
}