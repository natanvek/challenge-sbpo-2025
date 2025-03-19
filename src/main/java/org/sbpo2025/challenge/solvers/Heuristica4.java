package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.PriorityQueue;

import org.apache.commons.lang3.time.StopWatch;

public interface Heuristica4 {
class Aisle {
    public int aisleIdx;
    public double delta;
    public Map<Integer, Integer> orderElems;
    
    Aisle(int aisleIdx, double delta, Map<Integer, Integer> orderElems) {
        this.aisleIdx = aisleIdx;
        this.delta = delta;
        this.orderElems = orderElems;
    };
}

class Order {
    public boolean valid;
    public int orderIdx;
    public double delta;
    public int nElems;
    public Set<Aisle> aisles;

    Order(boolean valid, int orderIdx, double delta, int nElems, Set<Aisle> aisles) {
        this.valid = valid;
        this.orderIdx = orderIdx;
        this.delta = delta;
        this.nElems = nElems;
        this.aisles = aisles;
    }

    Set<Integer> aisleIdxs() {
        Set<Integer> idxs = new HashSet<>();
        for (Aisle aisle : aisles) {
            idxs.add(aisle.aisleIdx);
        }
        return idxs;
    }

    int intersectingAislesCount(Set<Integer>  idxs) {
        Set<Integer> ownIdxs = aisleIdxs();
        ownIdxs.retainAll(idxs);
        return ownIdxs.size();
    }

    void replace(Order other) {
        this.valid = other.valid;
        this.orderIdx = other.orderIdx;
        this.delta = other.delta;
        this.nElems = other.nElems;
        this.aisles = other.aisles; // shallow
    }
}
public class Solver extends ChallengeSolver {
    public Solver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems,
            int waveSizeLB, int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    @Override
    public String getName() {
        return "Heuristica4";
    }

    private boolean takeOrder(List<Map<Integer, Integer>> aislesState /* mutable */, Order order) {
        Map<Integer, Integer> orderMap = orders.get(order.orderIdx);
        // check es valida
        List<Map<Integer, Integer>> aislesStateCopy = new ArrayList<>(aislesState.size());
        for (Map<Integer, Integer> aisleMap : aislesState) {
            aislesStateCopy.add(new HashMap<>(aisleMap));
        }
        boolean ok = true;
        for (Map.Entry<Integer, Integer> entry : orderMap.entrySet()) {
            int elem = entry.getKey();
            int amount = entry.getValue();
            for (Aisle aisle : order.aisles) {
                Map<Integer, Integer> aisleMap = aislesStateCopy.get(aisle.aisleIdx);
                int aisleAmount = aisleMap.getOrDefault(elem, 0);
                int take = Math.min(aisleAmount, amount);
                aisleMap.put(elem, aisleAmount - take);
                amount -= take;
                if (amount == 0) {
                    break;
                }
            }
            if (amount > 0) {
                ok = false;
                break;
            }
        }
        if (!ok) {
            return false;
        }
        aislesState.clear();
        aislesState.addAll(aislesStateCopy);
        return true;
    }

    private Order routeOrder(List<Map<Integer, Integer>> aislesState /* mutable */, Set<Integer> currAisles, int orderIdx) {
        Map<Integer, Integer> orderMap = orders.get(orderIdx);
        // calcular orden
        int nElems = 0; 
        for (Map.Entry<Integer, Integer> entry : orderMap.entrySet()) {
            nElems += entry.getValue();
        }
        // check es valida
        if (nElems > waveSizeUB) {
            return new Order(false, orderIdx, 0, 0, null);
        }
        // selección de pasillos
        // heuristica: prefiero los pasillos que mejor se ajusten a la orden
        Set<Aisle> selectedAisles = new HashSet<>();
        PriorityQueue<Aisle> aislesHeap = new PriorityQueue<>((a, b) -> Double.compare(b.delta, a.delta));
        for (int aisleIdx = 0; aisleIdx < aislesState.size(); ++aisleIdx) {
            Map<Integer, Integer> aisle = aislesState.get(aisleIdx);
            double aisleDelta = 0;
            Map<Integer, Integer> orderElems = new HashMap<>();
            for (Map.Entry<Integer, Integer> entry : orderMap.entrySet()) {
                int elem = entry.getKey();
                int aisleAmount = aisle.getOrDefault(elem, 0);
                // heuristica: mayor delta si tiene de sobra
                aisleDelta += aisleAmount; 
                orderElems.put(elem, aisleAmount);
            }
            if (currAisles != null && currAisles.contains(aisleIdx)) {
                aisleDelta = Double.POSITIVE_INFINITY;
            }
            aislesHeap.add(new Aisle(aisleIdx, aisleDelta, orderElems));
        }
        int nElemsCopy = nElems;
        Map<Integer, Integer> orderMapCopy = new HashMap<Integer, Integer>(orderMap);
        while (nElemsCopy > 0 && !aislesHeap.isEmpty()) {
            Aisle aisle = aislesHeap.poll();
            selectedAisles.add(aisle);
            for (Map.Entry<Integer, Integer> entry : aisle.orderElems.entrySet()) {
                int elem = entry.getKey();
                int aisleAmount = entry.getValue();
                int orderAmount = orderMapCopy.getOrDefault(elem, 0);
                int take = Math.min(aisleAmount, orderAmount);
                aisle.orderElems.put(elem, take);
                orderMapCopy.put(elem, orderAmount - take);
                nElemsCopy -= take;
            }
        }
        if (selectedAisles.size() == 0 || nElemsCopy > 0) {
            return new Order(false, orderIdx, 0, 0, null);
        }
        // heuristica: prefiero ordenes según #prod / #(pasillos seleccionados)
        return new Order(true, orderIdx, (double) nElems / selectedAisles.size(), nElems, selectedAisles);
    }

    private Order best(List<Order> ordersArray) {
        Order max = null;
        for (Order order : ordersArray) {
            if (max == null || Double.compare(order.delta, max.delta) == 1) {
                max = order;
            }
        }
        return max;
    }

    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        int ordersSize = orders.size();
        List<Map<Integer, Integer>> aislesState = new ArrayList<>(aisles.size());
        for (Map<Integer, Integer> aisle: aisles) {
            aislesState.add(new HashMap<>(aisle));
        }
        List<Order> ordersArray = new ArrayList<>(ordersSize);
        for (int orderIdx = 0; orderIdx < ordersSize; ++orderIdx) {
            ordersArray.add(routeOrder(aislesState, null, orderIdx));
        }
        // heuristica: greedy y en cada paso actualizo el delta de todas las ordenes
        Set<Integer> ansOrders = new HashSet<>();
        Set<Integer> ansAisles = new HashSet<>();
        int elems = 0;

        System.out.println("order,delta,elems,aisles,totalElems,totalAisles,currObj");
        while (ansOrders.size() < waveSizeUB) {
            Order order = best(ordersArray);
            if (order == null || Double.compare(order.delta, Double.NEGATIVE_INFINITY) == 0) {
                break;
            }
            if (elems + order.nElems > waveSizeUB) {
                //System.out.println("skipping no space");
                order.delta = Double.NEGATIVE_INFINITY;
                continue;
            }
            if (!order.valid) {
                //System.out.println("skipping invalid");
                order.delta = Double.NEGATIVE_INFINITY;
                continue;
            }
            boolean ok = takeOrder(aislesState, order);
            if (!ok) {
                //System.out.println("skipping no longer valid");
                order.delta = Double.NEGATIVE_INFINITY;
                continue;
            }
            // if (elems > waveSizeUB * 0.7) { // heuristica: cortar si ya no hay beneficio
            //     Set<Integer> ansAislesCopy = new HashSet<>(ansAisles);
            //     ansAislesCopy.addAll(order.aisleIdxs());
            //     double currValue = elems / ansAisles.size();
            //     double newValue = (elems + order.nElems) / ansAislesCopy.size();
            //     if (newValue < currValue) {
            //         break;
            //     }
            // }
            System.out.println(String.format("%d,%f,%d,%d,%d,%d,%f", order.orderIdx, order.delta, order.nElems, order.aisles.size(), elems, ansAisles.size(), (double) elems / ansAisles.size()));
            ansOrders.add(order.orderIdx);
            ansAisles.addAll(order.aisleIdxs());
            order.delta = Double.NEGATIVE_INFINITY;
            elems += order.nElems;
            for (Order other : ordersArray) {
                if (!other.valid || Double.compare(other.delta, Double.NEGATIVE_INFINITY) == 0) {
                    continue;
                }
                // heuristica: calculamos una nueva ruta, dando peso a los aisles actuales
                // other.replace(routeOrder(aislesState, ansAisles, other.orderIdx));
                // if (!other.valid) {
                //     continue;
                // }
                // heuristica: damos un bonus a ordenes que usan los pasillos que ya usamos
                int status = other.aisles.size() - other.intersectingAislesCount(ansAisles);
                if (status == 0) {
                    other.delta = Double.POSITIVE_INFINITY; 
                } else {
                    other.delta = other.nElems / status;
                }
            }
        }
        System.out.println(String.format("RESULT. elems: %d <= %d <= %d, pasillos: %d, objetivo: %f", waveSizeLB, elems, waveSizeUB, ansAisles.size(), (double) elems / ansAisles.size()));
        ChallengeSolution sol = new ChallengeSolution(ansOrders, ansAisles);
        boolean valid = isSolutionFeasible(sol);
        System.out.println(String.format("valid: %b", valid));
        return sol;
    }
}
}