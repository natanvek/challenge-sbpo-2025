package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

public class Heuristica3b extends Heuristica {
    public Heuristica3b(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems,
            int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }


    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        Arrays.sort(orders, (o1, o2) -> Integer.compare(o2.size, o1.size));

        Cart rta = new Cart();
        Cart actual = new Cart();
        int tope = as - 1;
        while (actual.my_aisles.size() <= tope) {
            Aisle best_a = null;
            int ocupaMx = -1;
            Cart rta_parcial = new Cart();
            for (Aisle a : aisles) {
                if (actual.my_aisles.contains(a.id))
                    continue;

                Cart copia = new Cart(actual);
                copia.addAisle(a);

                int ocupa = 0;
                for (Order o : orders)
                    for (Map.Entry<Integer, Integer> entry : o.items.entrySet())
                        ocupa += Math.min(copia.available.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());

                if (ocupaMx < ocupa) {
                    ocupaMx = ocupa;
                    best_a = a;
                }

                copia.fill();
                copia.removeRedundantAisles();

                if (copia.cantItems >= waveSizeLB)
                    rta_parcial.update(copia);
            }

            actual.addAisle(best_a);

            if (rta_parcial.cantItems >= waveSizeLB)
                rta.update(rta_parcial);

            tope = Math.min(tope, rta.getTope());
        }

        return new ChallengeSolution(rta.my_orders, rta.my_aisles);
    }
}