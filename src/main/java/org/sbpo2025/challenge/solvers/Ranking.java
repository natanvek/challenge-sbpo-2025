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

    public boolean insertCart(PriorityQueue<EfficientCart> queue, EfficientCart newCart, long regSize) {
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

    long calcRegisterSize(StopWatch stopWatch, long minutos) {
        long ti = stopWatch.getNanoTime(), tiempo_iterando = 0, iteraciones = 0;
        EfficientCart ECWithTopeAisles = new EfficientCart();
        for (int t = 0; t < tope; ++t)
            ECWithTopeAisles.addAisle(aisles[t]);

        for (; tiempo_iterando < 5e9; tiempo_iterando = (stopWatch.getNanoTime() - ti), ++iteraciones) {
            EfficientCart copia = new EfficientCart(ECWithTopeAisles);
            fill(copia);
        }
        double t_fill = (double) tiempo_iterando / (iteraciones * 1e6);
        long rs = (long) ((minutos * 60 * 1e4) / (t_fill * tope * nAisles));

        return Math.max(Math.min(rs, 1500), 1);
    }

    long registerSize = 1;

    void init() {
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
    }

    void ranking(List<PriorityQueue<Heuristica.EfficientCart>> rankings) {
        for (Aisle p : aisles)
            for (int r = tope - 1; r >= 0; --r)
                for (EfficientCart m : rankings.get(r)) {
                    EfficientCart copia = new EfficientCart(m);
                    copia.addAisle(p);
                    fill(copia);
                    updateRta(copia);
                    insertCart(rankings.get(r + 1), copia, registerSize);
                }

    }


    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        init();

        List<PriorityQueue<Heuristica.EfficientCart>> rankings = new ArrayList<>();
        for (int i = 0; i <= tope; i++)
            rankings.add(new PriorityQueue<>(Comparator.comparingInt(EfficientCart::getCantItems)));

        insertCart(rankings.get(0), new EfficientCart(), registerSize);

        long ti = stopWatch.getNanoTime();
        ranking(rankings);
        long tiempoPorRegisterSize = (long) ((stopWatch.getNanoTime() - ti) / 1e6);

        for (int i = 1; i <= tope; i++)
            rankings.set(i, new PriorityQueue<>(Comparator.comparingInt(EfficientCart::getCantItems)));


        long minutosDeEjecucion = 3;
        registerSize = (long) ((minutosDeEjecucion * 60 * 1e3) / tiempoPorRegisterSize);
        registerSize = Math.min(registerSize, 1000);
        System.out.println("registerSize: " + registerSize);

        if (registerSize > 1) {
            ranking(rankings);
        }

        return getSolution();
    }
}