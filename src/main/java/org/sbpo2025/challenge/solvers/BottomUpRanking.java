package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.nio.file.*;

import org.apache.commons.lang3.time.StopWatch;

public class BottomUpRanking extends Heuristica {
    public BottomUpRanking(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aislesh, int nItems, int waveSizeLB,
            int waveSizeUB) {
        super(orders, aislesh, nItems, waveSizeLB, waveSizeUB);
    }

    int registerSize = 15;

    public boolean insertCart(PriorityQueue<EfficientCart> queue, EfficientCart newCart) {
        if (queue.size() < registerSize) {
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

        PriorityQueue<EfficientCart>[] rankings = (PriorityQueue<EfficientCart>[]) new PriorityQueue[as + 1];
        for (int i = 0; i <= as; i++)
            rankings[i] = new PriorityQueue<>(Comparator.comparingInt(EfficientCart::getCantItems));

        insertCart(rankings[0], new EfficientCart());

        int tope = as - 1;
        for (int r = 0; r <= tope; ++r) { // si vas de 0 a tope no funca
            PriorityQueue<EfficientCart> guardo = new PriorityQueue<>(
                    Comparator.comparingInt(EfficientCart::getCantItems));
            for (EfficientCart m : rankings[r]) { // podría recordar el available
                for (Aisle p : aisles) {
                    if (m.hasAisle(p))
                        continue;
                    EfficientCart copia = new EfficientCart(m);
                    copia.resetAisles();
                    copia.addAisle(p);
                    copia.fill();
                    insertCart(rankings[r + 1], copia);

                    EfficientCart copia2 = new EfficientCart(copia);
                    copia2.removeRedundantAisles(); // just in case

                    if (copia2.cantItems >= waveSizeLB)
                        tope = Math.min(tope, copia.getTope());

                    if (copia2.aisleCount() == r + 1 || !copia2.hasAisle(p))
                        continue;

                    if (copia2.aisleCount() == r)
                        insertCart(guardo, copia2);
                    else
                        insertCart(rankings[copia2.aisleCount()], copia2);
                }
            }
            for (EfficientCart m : guardo)
                insertCart(rankings[r], m);
        }

        EfficientCart rta = new EfficientCart();

        for (int r = tope; r >= 0; --r)
            for (EfficientCart m : rankings[r])
                if (m.cantItems >= waveSizeLB)
                    rta.update(m);

        Cart rtaFinal = new Cart();

        for (int a : rta.my_aisles)
            rtaFinal.addAisle(idToAisle[a]);

        rtaFinal.fill();
        return new ChallengeSolution(rtaFinal.my_orders, rtaFinal.my_aisles);
    }
}