package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

public class Heuristica3 extends Heuristica {
    public Heuristica3(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems,
            int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }


    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        Arrays.sort(orders, (o1, o2) -> Integer.compare(o2.size, o1.size));

        Cart rta = new Cart();
        Cart actual = new Cart();
        int tope = as;
        while (actual.my_aisles.size() < tope) {
            Aisle best_a = null;
            Cart rta_parcial = new Cart();
            for (Aisle a : aisles) {
                if (actual.my_aisles.contains(a.id))
                    continue;

                Cart copia = new Cart(actual);
                copia.addAisle(a);
                copia.fill();
                copia.removeRedundantAisles();

                if (best_a == null)
                    best_a = a;

                if (rta_parcial.cantItems < waveSizeLB ||
                        (rta_parcial.cantItems >= waveSizeLB && copia.cantItems >= waveSizeLB)) {
                    if (rta_parcial.update(copia))
                        best_a = a; // es gracioso que sacar esta linea parece funcar decente
                }

            }
            actual.addAisle(best_a);
            if (rta_parcial.cantItems >= waveSizeLB)
                rta.update(rta_parcial);
            tope = Math.min(tope, rta.getTope());
        }

        return new ChallengeSolution(rta.my_orders, rta.my_aisles);
    }
}