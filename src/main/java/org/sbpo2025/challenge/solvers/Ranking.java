package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

public class Ranking extends Heuristica {
    public Ranking(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aislesh, int nItems, int waveSizeLB,
            int waveSizeUB) {
        super(orders, aislesh, nItems, waveSizeLB, waveSizeUB);
    }

    @Override
    public String getName() {
        return "Ranking"; // Nombre propio de la subclase
    }

    int registerSize = 15;

    public boolean insertCart(PriorityQueue<Cart> queue, Cart newCart) {
        if (queue.size() < registerSize) {
            queue.offer(newCart);
            return true;
        }

        Cart worstCart = queue.peek();

        if (worstCart.getValue() < newCart.getValue()) {
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

        Cart rta = new Cart();

        PriorityQueue<Cart>[] rankings = (PriorityQueue<Cart>[]) new PriorityQueue[as];
        for (int i = 0; i < as; i++)
            rankings[i] = new PriorityQueue<>(Comparator.comparingDouble(Cart::getValue));

        insertCart(rankings[0], new Cart());

        int tope = as - 1;
        for (Aisle p : aisles) {
            for (int r = tope-1; r >= 0; r--) { // si vas de 0 a tope no funca
                for (Cart m : rankings[r]) {
                    Cart copia = new Cart(m);
                    copia.resetOrders();
                    copia.addAisle(p);

                    copia.fill();
                    // copia.removeRedundantAisles(); trae problemas

                    if (copia.cantItems >= waveSizeLB)
                        tope = Math.min(tope, copia.getTope());

                    insertCart(rankings[r+1], copia);
                }
            }
        }

        for (int r = 0; r < as; ++r)
            for (Cart m : rankings[r])
                if (m.cantItems >= waveSizeLB){
                    m.removeRedundantAisles(); // just in case
                    rta.update(m);
                }


        return new ChallengeSolution(rta.my_orders, rta.my_aisles);
    }
}