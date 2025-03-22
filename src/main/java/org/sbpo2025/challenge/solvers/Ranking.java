package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.AisleSet;
import org.sbpo2025.challenge.AisleSet;
import org.sbpo2025.challenge.AisleSet;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

public class Ranking extends Heuristica {
    public Ranking(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aislesh, int nItems,
            int waveSizeLB,
            int waveSizeUB) {
        super(orders, aislesh, nItems, waveSizeLB, waveSizeUB);
    }

    public boolean insertCart(PriorityQueue<AisleSet> queue, AisleSet newCart, Integer regSize) {
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
        Arrays.sort(aisles, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));

        int[] idToPos = new int[nAisles];
        for (int a = 0; a < nAisles; ++a)
            idToPos[aisles[a].id] = a;

        // Cart iniciarTope = pasada(as);

        int tope = nAisles;
        // long ti = stopWatch.getNanoTime();
        // int iteraciones = 100;
        // for(int it = 0; it < iteraciones; ++it){
        // AisleSet estimatingRegisterSize = AisleSet.nullAisleSet();
        // for(Aisle a : aisles)
        // estimatingRegisterSize = new AisleSet(estimatingRegisterSize, a);

        // fill(estimatingRegisterSize);
        // }
        // long delta_t = (stopWatch.getNanoTime() - ti) / iteraciones;

        int registerSize = 5;
        // int registerSize = (int) ((MAX_RUNTIME * 1e6) / (delta_t * tope * as));

        System.out.println("register size: " + registerSize);

        PriorityQueue<AisleSet>[] rankings = (PriorityQueue<AisleSet>[]) new PriorityQueue[nAisles + 1];
        for (int i = 0; i <= nAisles; i++)
            rankings[i] = new PriorityQueue<>(Comparator.comparingInt(AisleSet::getCantItems));

        insertCart(rankings[0], new AisleSet(), registerSize);

        // System.out.println("Pasillos: " + as);
        for (Aisle p : aisles) {
            // System.out.print("\rTopeActual: " + tope);

            for (int r = tope - 1; r >= 0; --r) { // si vas de 0 a tope no funca
                for (AisleSet m : rankings[r]) {
                    AisleSet copia = new AisleSet(m);
                    copia.addAisle(p);
                    copia.setAvailable();
                    
                    fill(copia);


                    if (copia.cantItems >= waveSizeLB)
                        tope = Math.min(tope, getTope(copia));

                    insertCart(rankings[r + 1], copia, registerSize);
                }
            }
        }

        // System.out.println();

        int guardo = 100;
        AisleSet rta = new AisleSet();
        PriorityQueue<AisleSet> top10 = new PriorityQueue<>(Comparator.comparingDouble(AisleSet::getValue));
        for (int r = tope; r >= 0; --r)
            for (AisleSet m : rankings[r])
                if (m.cantItems >= waveSizeLB) {
                    rta = (AisleSet) updateAnswer(rta, m);
                    insertCart(top10, m, guardo);
                }

        System.out.print("[");

        for (Aisle a : rta)
            System.out.print(idToPos[a.id] + ", ");

        System.out.println("]");

        return getSolution(rta);
    }
}