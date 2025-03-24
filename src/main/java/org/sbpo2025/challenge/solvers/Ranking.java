package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

public class Ranking extends Heuristica {
    public Ranking(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int _nItems,
            int _waveSizeLB,
            int _waveSizeUB) {
        super(_orders, _aisles, _nItems, _waveSizeLB, _waveSizeUB);
    }

    public boolean insertCart(PriorityQueue<EfficientCart> queue, EfficientCart newCart, Integer regSize) {
        if (queue.size() < regSize) {
            queue.offer(newCart);
            return true;
        }

        if (queue.peek().getValue() < newCart.getValue()) {
            queue.poll();
            queue.offer(newCart);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        int[] pesosAisle = new int[nAisles];

        for (Order o : orders) {
            if (o.size > waveSizeUB)
                continue;
            for (Aisle p : aisles) {
                int ocupa = 0;

                for (Map.Entry<Integer, Integer> entry : o.items)
                    ocupa += Math.min(p.map_items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());

                pesosAisle[p.id] += ocupa;
            }
        }

        Arrays.sort(orders, (o1, o2) -> Integer.compare(o2.size, o1.size));
        Arrays.sort(aisles, (a1, a2) -> Integer.compare(a2.size, a1.size));
        pasada();

        Arrays.sort(aisles, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));
        pasada();

        int[] idToPos = new int[nAisles];
        for (int a = 0; a < nAisles; ++a)
            idToPos[aisles[a].id] = a;


        // ----------------------------------------------------------------------------------
        

        // long ti = stopWatch.getNanoTime();
        // int iteraciones = 100;
        // for (int it = 0; it < iteraciones; ++it) {
        //     EfficientCart simulatingBest = new EfficientCart();
        //     for(int r = 0; r < tope; ++r){
        //         EfficientCart estimatingRegisterSize = new EfficientCart(simulatingBest);
        //         simulatingBest.addAisle(aisles[r]);
        //         fill(estimatingRegisterSize);
        //         simulatingBest = estimatingRegisterSize;
        //     }
        // }
        // long delta_t = (stopWatch.getNanoTime() - ti) / iteraciones;
        // System.out.println("delta_t: "+ (delta_t / 1e6));
        // int registerSize = (int) ((MAX_RUNTIME * 1e6 / 5) / (delta_t * nAisles));

        
        
        int registerSize = 5;
        System.out.println("registerSize: "+ registerSize);

        // ----------------------------------------------------------------------------------

        PriorityQueue<EfficientCart>[] rankings = (PriorityQueue<EfficientCart>[]) new PriorityQueue[nAisles + 1];
        for (int i = 0; i <= nAisles; i++)
            rankings[i] = new PriorityQueue<>(Comparator.comparingInt(EfficientCart::getCantItems));

        insertCart(rankings[0], new EfficientCart(), registerSize);

        // System.out.println("Pasillos: " + as);
        for (Aisle p : aisles) {
            // System.out.print("\rTopeActual: " + tope);
            for (int r = tope - 1; r >= 0; --r) { // si vas de 0 a tope no funca
                for (EfficientCart m : rankings[r]) {
                    EfficientCart copia = new EfficientCart(m);
                    copia.addAisle(p);
                    // copia.fill();

                    fill(copia);

                    updateRta(copia);

                    insertCart(rankings[r + 1], copia, registerSize);
                }

            }

        }
        // System.out.println();

        // int guardo = 100;

        // PriorityQueue<EfficientCart> top10 = new PriorityQueue<>(Comparator.comparingDouble(EfficientCart::getValue));
        // for (int r = tope; r >= 0; --r)
        //     for (EfficientCart m : rankings[r])
        //         if (m.cantItems >= waveSizeLB) {
        //             rta.update(m);
        //             insertCart(top10, m, guardo);
        //         }

        return getSolution();
    }
}