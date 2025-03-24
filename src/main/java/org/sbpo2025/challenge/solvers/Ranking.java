package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

public class Ranking extends Heuristica {
    public Ranking(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aislesh, int nItems,
            int waveSizeLB,
            int waveSizeUB) {
        super(orders, aislesh, nItems, waveSizeLB, waveSizeUB);
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
        int[] pesosAisle = new int[as];

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
        Arrays.sort(aisles, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));
        Cart iniciarTope = pasada(as);

        int[] idToPos = new int[as];
        for (int a = 0; a < as; ++a)
            idToPos[aisles[a].id] = a;

        PriorityQueue<EfficientCart>[] rankings = (PriorityQueue<EfficientCart>[]) new PriorityQueue[as + 1];
        for (int i = 0; i <= as; i++)
            rankings[i] = new PriorityQueue<>(Comparator.comparingInt(EfficientCart::getCantItems));

        int registerSize = 5;

        insertCart(rankings[0], new EfficientCart(), registerSize);

        int tope = iniciarTope.getTope();

        // System.out.println("Pasillos: " + as);
        for (Aisle p : aisles) {
            // System.out.print("\rTopeActual: " + tope);
            for (int r = tope - 1; r >= 0; --r) { // si vas de 0 a tope no funca
                for (EfficientCart m : rankings[r]) {
                    EfficientCart copia = new EfficientCart(m);
                    copia.addAisle(p);
                    copia.fill();

                    if (copia.cantItems >= waveSizeLB)
                        tope = Math.min(tope, copia.getTope());

                    insertCart(rankings[r + 1], copia, registerSize);
                }
            }
        }
        // System.out.println();

        int guardo = 100;
        EfficientCart rta = new EfficientCart();
        PriorityQueue<EfficientCart> top10 = new PriorityQueue<>(Comparator.comparingDouble(EfficientCart::getValue));
        for (int r = tope; r >= 0; --r)
            for (EfficientCart m : rankings[r])
                if (m.cantItems >= waveSizeLB) {
                    rta.update(m);
                    insertCart(top10, m, guardo);
                }

        // int optimos = 0;
        // int aparecen = 0;
        // int[] pasillosOptimos = new int[as];
        // for (EfficientCart m : top10)
        //     for (Integer id : m.getAisles()) {
        //         pasillosOptimos[id] += 1;

        //         if (pasillosOptimos[id] == 1)
        //             ++aparecen;

        //         if (pasillosOptimos[id] == guardo)
        //             ++optimos;
        //     }

        // System.out.println("Optimos: " + optimos);
        // System.out.println("Quedan: " + (aparecen - optimos));
        // System.out.println("Tope: " + (tope));
        // Aisle[] aisles2 = new Aisle[aparecen - optimos];
        // int last = 0;
        // for (int id = 0; id < as; ++id) {
        //     if (0 < pasillosOptimos[id] && pasillosOptimos[id] < guardo) {
        //         aisles2[last++] = idToAisle[id];
        //     } else if (pasillosOptimos[id] == guardo) {
        //         for (Map.Entry<Integer, Integer> entry : idToAisle[id].items.entrySet())
        //             available_inicial[entry.getKey()] += entry.getValue();

        //         aisles_iniciales.add(id);
        //     }
        // }

        // System.out.println("Aisle2.size(): " + last);

        // for (int i = optimos; i <= as; i++)
        //     rankings[i] = new PriorityQueue<>(Comparator.comparingInt(EfficientCart::getCantItems));

        // registerSize = 2000;

        // insertCart(rankings[optimos], new EfficientCart(), registerSize);

        // for (Aisle p : aisles2) {
        //     // System.out.print("\rTopeActual: " + tope);
        //     for (int r = tope - 1; r >= optimos; --r) { // si vas de 0 a tope no funca
        //         for (EfficientCart m : rankings[r]) {
        //             EfficientCart copia = new EfficientCart(m);
        //             copia.addAisle(p);
        //             copia.fill();

        //             if (copia.cantItems >= waveSizeLB)
        //                 tope = Math.min(tope, copia.getTope());

        //             insertCart(rankings[r + 1], copia, registerSize);
        //         }
        //     }
        // }

        // rta = new EfficientCart();
        // for (int r = tope; r >= optimos; --r)
        //     for (EfficientCart m : rankings[r])
        //         if (m.cantItems >= waveSizeLB)
        //             rta.update(m);

        Cart rtaFinal = new Cart();

        for (int a : rta.getAisles())
            rtaFinal.addAisle(idToAisle[a]);

        for (int a : aisles_iniciales)
            rtaFinal.addAisle(idToAisle[a]);
            
        System.out.print("[");
        
        for (int p : rtaFinal.my_aisles)
            System.out.print(idToPos[p]+", ");
        
        System.out.println("]");

        rtaFinal.fill();

        return new ChallengeSolution(rtaFinal.my_orders, rtaFinal.my_aisles);
    }
}