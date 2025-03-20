package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.nio.file.*;

import org.apache.commons.lang3.time.StopWatch;

public class Ranking extends Heuristica {
    public Ranking(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aislesh, int nItems,
            int waveSizeLB,
            int waveSizeUB) {
        super(orders, aislesh, nItems, waveSizeLB, waveSizeUB);
    }

    int registerSize = 30;

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
        Cart iniciarTope = pasada(as);

        PriorityQueue<EfficientCart>[] rankings = (PriorityQueue<EfficientCart>[]) new PriorityQueue[as + 1];
        for (int i = 0; i <= as; i++)
            rankings[i] = new PriorityQueue<>(Comparator.comparingInt(EfficientCart::getCantItems));

        insertCart(rankings[0], new EfficientCart());

        int tope2 = iniciarTope.getTope();
        int tope = as;
        // int pasos = as;

        System.out.println("Pasillos: " + as);
        System.out.println("TopeH2: " + tope2);
        for (Aisle p : aisles) {
            System.out.print("\rTopeActual: " + tope);
            for (int r = tope-1; r >= 0; --r) { // si vas de 0 a tope no funca
                for (EfficientCart m : rankings[r]) {
                    EfficientCart copia = new EfficientCart(m);
                    copia.addAisle(p);
                    copia.fill();

                    if (copia.cantItems >= waveSizeLB)
                        tope = Math.min(tope, copia.getTope());

                    insertCart(rankings[r+1], copia);
                }
            }
        }

        EfficientCart rta = new EfficientCart();
        PriorityQueue<EfficientCart> top10 = new PriorityQueue<>(Comparator.comparingDouble(EfficientCart::getValue));
        for (int r = tope; r >= 0; --r)
            for (EfficientCart m : rankings[r])
                if (m.cantItems >= waveSizeLB){
                    rta.update(m);
                    insertCart(top10, m);
                }


        System.out.println("TOP10 - Indices en Pesos");
        for(EfficientCart m : top10){
            List<Integer> indices = new ArrayList<>(0);
            int index = 0;
            for(Aisle p : aisles) {
                if(m.hasAisle(p))
                    indices.add(index);
                ++index;
            }
            System.out.println(indices);

        }

                
        Cart rtaFinal = new Cart();

        for (int a : rta.getAisles())
            rtaFinal.addAisle(idToAisle[a]);

        rtaFinal.fill();


        System.out.println(String.format("medirFill: %.2e", (double) medirFill));
        System.out.println(String.format("medirSetAvailable: %.2e", (double) medirSetAvailable));
        System.out.println(String.format("medirCopy: %.2e", (double) medirCopy));
        return new ChallengeSolution(rtaFinal.my_orders, rtaFinal.my_aisles);
    }
}