package org.sbpo2025.challenge.solvers;  // Paquete correcto

import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;

import org.apache.commons.lang3.time.StopWatch;

public class Heuristica6 extends Heuristica {
    public Heuristica6(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    
    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        int[] pesosAisle = new int[as];

        Cart rta = pasada(as);
        
        Cart[] pasillosOptimos_o = new Cart[os];


        /*
         * esto encuentra los pasillos "optimos" con alguna heuristica para cada orden y le suma un peso
         * a los pasillos que aparecene en la "solucion"
         * investigar buenas heuristicas para el problema de la mochila entera
         */
        for(int o = 0; o < os; ++o) {     
            pasillosOptimos_o[o] = new Cart();     
            if(orders[o].size > waveSizeUB) continue;

            Map<Integer, Integer> copia_o = orders[o].items;
            int ocuparon = 0;
            while(ocuparon < orders[o].size) {
                int mxocupa = -1, pmx = -1;
                for(int p = 0; p < as; ++p) {
                    // hacer set disponible para optimizar complejidad
                    if(pasillosOptimos_o[o].my_aisles.contains(p)) continue; 
                    int ocupa = 0;
                    for (Map.Entry<Integer, Integer> entry : copia_o.entrySet()) {
                        ocupa += Math.min(aisles[p].items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());
                    }
                    if(ocupa > mxocupa) {
                        mxocupa = ocupa;
                        pmx = p;
                    }
                }

                for (Map.Entry<Integer, Integer> entry : copia_o.entrySet()) {
                    int meResta = Math.min(aisles[pmx].items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());
                    copia_o.merge(entry.getKey(), -meResta, Integer::sum);
                    ocuparon += meResta;
                }

                pasillosOptimos_o[o].addAisle(aisles[pmx]);
                
                pesosAisle[pmx] += mxocupa;

            }

            pasillosOptimos_o[o].removeRequestIfPossible(orders[o].items);
            pasillosOptimos_o[o].cantItems += orders[o].size;
            pasillosOptimos_o[o].my_orders.add(orders[o].id);

            if (pasillosOptimos_o[o].cantItems >= waveSizeLB)
                rta.update(pasillosOptimos_o[o]);

            // for (int p : pasillosOptimos_o[o].my_aisles) {
            //     pesosAisle[p] += (Integer) ordersh[o].size ;
            // }
        }


        Arrays.sort(orders, (o1, o2) -> Integer.compare(o2.size, o1.size));
        Arrays.sort(aisles, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));
        rta.update(pasada(as-1));
        

        /*
         * considerar otras opciones que no sean solo pasada, como se complementan las optimas?
         */
        // Arrays.sort(aislesh, (a1, a2) -> Integer.compare(a2.size, a1.size));


        return new ChallengeSolution(rta.my_orders, rta.my_aisles);
    }
}