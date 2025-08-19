package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;

import scala.Int;

import org.sbpo2025.challenge.ChallengeSolution;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

public class Sampling extends Heuristica {
    public Sampling(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int _nItems,
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

                for (Map.Entry<Integer, Integer> entry : o.items.entrySet())
                    ocupa += Math.min(p.items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());

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

        int registerSize = 5;
        // int registerSize = Math.max(1, calcRegisterSize(stopWatch, 8));
        System.out.println("registerSize: " + registerSize);
        System.out.println("rta: " + rta.getValue());

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

        System.out.println("rta: " + rta.getValue());

        int samples = 100000;

        int sumaOrders = 0;
        for (Order o : orders)
            sumaOrders += o.size;

        System.out.println("sumaOrders: " + sumaOrders);

        PriorityQueue<Heuristica.EfficientCart> bestSizes = new PriorityQueue<>(
                Comparator.comparingInt(EfficientCart::getCantItems));

        for (int r = 1; r <= tope; ++r) {
            PriorityQueue<Heuristica.EfficientCart> actual = new PriorityQueue<>(
                    Comparator.comparingInt(EfficientCart::getCantItems));
            for (EfficientCart m : rankings.get(r)) {
                if (m.getCantItems() < waveSizeLB)
                    continue;
                insertCart(actual, m, 1);
            }
            if (actual.isEmpty())
                continue;
            insertCart(bestSizes, actual.peek(), 5);
        }



        List<Integer> bestSizesList = new ArrayList<>();
        for (EfficientCart m : bestSizes) {
            bestSizesList.add(m.aisleCount()); 
        }



        for (int it = 0; it < 10; it++) {

            Set<Aisle> nextAislesVan = new HashSet<>();
            for (int r : bestSizesList) {
                for (EfficientCart m : rankings.get(r)) {
                    if (m.getCantItems() < waveSizeLB)
                        continue;
                    for (Aisle a : m.my_aisles)
                        nextAislesVan.add(a);
                }
            }

            Aisle[] aislesVan = new Aisle[nextAislesVan.size()];

            int pos = 0;
            for (Aisle a : nextAislesVan)
                aislesVan[pos++] = a;

            System.out.println("Iteration: " + it);
            System.out.println("aislesVan: " + aislesVan.length);

            List<Integer> perm = new ArrayList<>();
            for (int i = 0; i < aislesVan.length; i++)
                perm.add(i);
            for (int r : bestSizesList) {

                double top = 0;
                for (int s = 0; s < samples; ++s) {

                    Collections.shuffle(perm); // barajar
                    EfficientCart randomCart = new EfficientCart();
                    for (int i = 0; i < r; ++i)
                        randomCart.addAisle(aislesVan[perm.get(i)]);

                    fill(randomCart);
                    updateRta(randomCart);
                    insertCart(rankings.get(r), randomCart, registerSize);

                }


                for (EfficientCart m : rankings.get(r)) top = Math.max(top, m.getValue());

                System.out.println("r: " + r + " | top: " + top);
            }

        }

        System.out.println("Tope: " + tope);
        System.out.println("nAisles: " + nAisles);
        System.out.println("aislesInSolution: " + rta.getAisles().size());
        return getSolution();
    }
}