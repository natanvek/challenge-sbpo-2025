package org.sbpo2025.challenge.solvers;  // Paquete correcto

import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;

import org.apache.commons.lang3.time.StopWatch;

public class H2yShuffleOrders extends Heuristica {
    public H2yShuffleOrders(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    @Override
    public String getName() {
        return "H2yShuffleOrders";  // Nombre propio de la subclase
    }
    
    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        int[] pesosAisle = new int[as];

        
        for(int o = 0; o < os; ++o) {          
            if(ordersh[o].size > waveSizeUB) continue;
            for(int p = 0; p < as; ++p) {
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

        for(int it = 0; it < 50; ++it) {
            System.out.println("iteracion "+it);
            List<Order> list = Arrays.asList(ordersh);  // Convertir el array a lista
            Collections.shuffle(list);  // Shuffle la lista
            ordersh = list.toArray(new Order[0]);

            for(int sol = 0; sol < as; ++sol) {
                Cart actual = new Cart();
                for(int p = 0; p <= sol; ++p)
                    actual.addAisle(p);
                
                for(int o = 0; o < os; ++o) {
                    if(actual.cantItems + ordersh[o].size <= waveSizeUB && actual.removeRequestIfPossible(ordersh[o].items)) {
                        actual.cantItems += ordersh[o].size;
                        actual.my_orders.add(ordersh[o].id);
                    }
                }
    
                for(int p = sol; p >= 0; --p) {
                    if(actual.my_aisles.contains(aislesh[p].id) && actual.removeRequestIfPossible(aislesh[p].items)) {
                        actual.my_aisles.remove(aislesh[p].id);
                    }
                }
        
                if(actual.cantItems >= waveSizeLB) rta.update(actual);
    
            }
        }
        


        return new ChallengeSolution(rta.my_orders, rta.my_aisles);
    }
}