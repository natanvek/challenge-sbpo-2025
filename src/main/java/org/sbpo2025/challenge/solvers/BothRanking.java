package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;


import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

public class BothRanking extends Heuristica {
    public BothRanking(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int _nItems,
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

    int calcRegisterSize(StopWatch stopWatch, long minutos) {
        long ti = stopWatch.getNanoTime(), tiempo_iterando = 0, iteraciones = 0;
        while(tiempo_iterando < 5e9) {
            for(int i = 0; i < 100; ++ i) {
                EfficientCart simulatingBest = new EfficientCart();
                for(int r = 0; r < tope; ++r){
                    EfficientCart estimatingRegisterSize = new EfficientCart(simulatingBest);
                    simulatingBest.addAisle(aisles[r]);
                    fill(estimatingRegisterSize);
                    simulatingBest = estimatingRegisterSize;
                }
            }
            tiempo_iterando = (stopWatch.getNanoTime() - ti);
            iteraciones += 100;
        }
        double tope_por_fill = (double) tiempo_iterando / (iteraciones * 1e6);
        return (int)((minutos * 60 * 1e3) / (tope_por_fill * nAisles));
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
        
        // int registerSize = 500;
        int registerSize = Math.min(1500, calcRegisterSize(stopWatch, 1));
        System.out.println("registerSize: "+ registerSize);

        // ----------------------------------------------------------------------------------

        List<PriorityQueue<Heuristica.EfficientCart>> rankings = new ArrayList<>();
        for (int i = 0; i <= tope; i++)
            rankings.add(new PriorityQueue<>(Comparator.comparingInt(EfficientCart::getCantItems)));

        insertCart(rankings.get(0), new EfficientCart(), registerSize);

        for (Aisle p : aisles) 
            for (int r = tope - 1; r >= 0; --r) 
                for (EfficientCart m : rankings.get(r)) {
                    EfficientCart copia = new EfficientCart(m);
                    copia.addAisle(p);
                    fill(copia);
                    updateRta(copia);
                    insertCart(rankings.get(r + 1), copia, registerSize);
                }

        for (int r = 0; r < tope; ++r) 
            for (EfficientCart m : rankings.get(r)) 
                for (Aisle p : aisles) {
                    if (m.hasAisle(p)) continue;

                    EfficientCart copia = new EfficientCart(m);
                    copia.addAisle(p);
                    fill(copia);
                    updateRta(copia);
                    insertCart(rankings.get(r+1), copia, registerSize);
                }

        return getSolution();
    }
}