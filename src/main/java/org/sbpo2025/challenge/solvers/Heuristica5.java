package org.sbpo2025.challenge.solvers;

import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.apache.commons.lang3.time.StopWatch;

public interface Heuristica5 {

class Aisle {
    public int aisleIdx;
    public boolean inUse;
    public double delta;
    public Map<Integer, Integer> elements;
    Aisle(
        int aisleIdx, 
        boolean inUse, 
        double delta, 
        Map<Integer, Integer> elements) {
        this.aisleIdx = aisleIdx;
        this.inUse = inUse;
        this.delta = delta;
        this.elements = elements;
    };
}

class Order {
    public int orderIdx;
    public boolean inUse;
    public double delta;
    public int nElems;
    public Map<Integer, Integer> elements;
    Order(
        int orderIdx,
         boolean inUse,
         double delta,
         int nElems,
         Map<Integer,Integer> elements) {
        this.orderIdx = orderIdx;
        this.inUse = inUse;
        this.delta = delta;
        this.nElems = nElems;
        this.elements = elements;
    }
}

class WorkingSet {
    Map<Integer, Integer> elems;
    Map<Integer, Order> selectedOrders;
    Map<Integer, Aisle> selectedAisles;
    int waveSizeUB;
    int nSelectedElems;
    int nSelectedAisles;
    private double objectiveValue;

    WorkingSet(int waveSizeUB) {
        this.waveSizeUB = waveSizeUB;
        elems = new HashMap<>();
        selectedAisles = new HashMap<>();
        selectedOrders = new HashMap<>();
        nSelectedElems = 0;
        nSelectedAisles = 0;
        objectiveValue = Double.NEGATIVE_INFINITY;
    }

    boolean canAddOrder(Order order) {
        if (selectedOrders.containsKey(order.orderIdx)) {
            return false;
        }
        if (nSelectedElems + order.nElems > waveSizeUB) {
            return false;
        }
        Map<Integer, Integer> orderElems = order.elements;
        for (Map.Entry<Integer, Integer> entry : orderElems.entrySet()) {
            int element = entry.getKey();
            int amount = entry.getValue();
            int available = elems.getOrDefault(element, 0);
           if (available < amount) {
                return false;
            }
        }
        return true;
    }

    boolean addOrder(Order order, boolean check) {
        if (selectedOrders.containsKey(order.orderIdx)) {
            return true;
        }
        if (check && !canAddOrder(order)) {
            return false;
        }
        Map<Integer, Integer> orderElems = order.elements;
        for (Map.Entry<Integer, Integer> entry : orderElems.entrySet()) {
            int elem = entry.getKey();
            int amount = entry.getValue();
            elems.compute(elem, (key, val) -> val - amount);
        }
        selectedOrders.put(order.orderIdx, order);
        nSelectedElems += order.nElems;
        order.inUse = true;
        updateObjectiveValue();
        return true;
    }

    boolean removeOrder(int orderIdx) {
        Order order = selectedOrders.get(orderIdx);
        if (order == null) {
            return false;
        }
        Map<Integer, Integer> orderElems = order.elements;
        for (Map.Entry<Integer, Integer> entry : orderElems.entrySet()) {
            int elem = entry.getKey();
            int amount = entry.getValue();
            elems.compute(elem, (key, val) -> val + amount);
        }
        selectedOrders.remove(order.orderIdx);
        nSelectedElems -= order.nElems;
        order.inUse = false;
        updateObjectiveValue();
        return true;
    }

    boolean canRemoveAisle(int aisleIdx) {
        Aisle aisle = selectedAisles.get(aisleIdx);
        if (aisle == null) {
            return false;
        }
        Map<Integer, Integer> aisleElems = aisle.elements;
        for (Map.Entry<Integer, Integer> entry : aisleElems.entrySet()) {
            int elem = entry.getKey();
            int amount = entry.getValue();
            int available = elems.getOrDefault(elem, 0);
            if (available < amount) {
                return false;
            }
        }
        return true;
    }

    boolean addAisle(Aisle aisle) {
        if (selectedAisles.containsKey(aisle.aisleIdx)) {
            return false;
        }
        Map<Integer, Integer> aisleElems = aisle.elements;
        for (Map.Entry<Integer, Integer> entry : aisleElems.entrySet()) {
            int elem = entry.getKey();
            int amount = entry.getValue();
            elems.compute(elem, 
                (key, val) -> (val == null) ? amount : val + amount);
        }
        selectedAisles.put(aisle.aisleIdx, aisle);
        nSelectedAisles += 1;
        aisle.inUse = true;
        updateObjectiveValue();
        return true;
    }

    boolean removeAisle(int aisleIdx, boolean check) {
        Aisle aisle = selectedAisles.get(aisleIdx);
        if (aisle == null) {
            return false;
        }
        if (check && !canRemoveAisle(aisleIdx)) {
            return false;
        }
        Map<Integer, Integer> aisleElems = aisle.elements;
        for (Map.Entry<Integer, Integer> entry : aisleElems.entrySet()) {
            int elem = entry.getKey();
            int amount = entry.getValue();
            elems.compute(elem, (key, val) -> val - amount);
        }
        selectedAisles.remove(aisle.aisleIdx);
        nSelectedAisles -= 1;
        aisle.inUse = false;
        updateObjectiveValue();
        return true;
    }

    void updateObjectiveValue() {
        if (nSelectedAisles == 0) {
            objectiveValue = Double.NEGATIVE_INFINITY;
        }
        objectiveValue = (double) nSelectedElems / nSelectedAisles;
    }
}

public class Solver extends ChallengeSolver {

    Order[] allOrders;
    Aisle[] allAisles;
    
    public Solver(
        List<Map<Integer, Integer>> orders, 
        List<Map<Integer, Integer>> aisles, 
        int nItems,
        int waveSizeLB, 
        int waveSizeUB) {
        super(orders, aisles, nItems, waveSizeLB, waveSizeUB);    
        allOrders = new Order[orders.size()];
        for (int i = 0; i < orders.size(); ++i) {
            int nElems = 0;
            Map<Integer, Integer> elems = orders.get(i);
            for (Map.Entry<Integer, Integer> entry : elems.entrySet()) {
                nElems += entry.getValue();
            }
            allOrders[i] = new Order(i, false, 0, nElems, elems);
        }
        allAisles = new Aisle[aisles.size()];
        for (int i = 0; i < aisles.size(); ++i) {
            allAisles[i] = new Aisle(i, false, 0, aisles.get(i));
        }
    }

    @Override
    public String getName() {
        return "Heuristica5";
    }
    
    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        for (Aisle aisle1 : allAisles) {
            for (Aisle aisle2 : allAisles) {
                WorkingSet ws = new WorkingSet(waveSizeUB);
                ws.addAisle(aisle1);
                ws.addAisle(aisle2);
                int aisleDelta = 0;
                for (Order order : allOrders) {
                    int orderDelta = 0;
                    if (ws.addOrder(order, true)) {
                        orderDelta += 1;
                    }
                    order.delta += orderDelta;
                    aisleDelta += orderDelta * order.nElems;
                }
                aisle1.delta += aisleDelta;
                aisle2.delta += aisleDelta;
            }
        }
        WorkingSet ws = new WorkingSet(waveSizeUB);
        Set<Integer> bestAisles = new HashSet<>();
        Set<Integer> bestOrders = new HashSet<>();
        double best = 0;
        Arrays.sort(allOrders, (o1, o2) -> Double.compare(o2.delta, o1.delta));
        Arrays.sort(allAisles, (a1, a2) -> Double.compare(a2.delta, a1.delta));
        for (Aisle aisle : allAisles) {
            ws.addAisle(aisle);
            for (Order order : allOrders) {
                ws.addOrder(order, true);
            }
            // System.out.println(String.format("%d, %d, %f", 
            //     ws.nSelectedElems, ws.nSelectedAisles, ws.objectiveValue));
            if (ws.objectiveValue > best) {
                best = ws.objectiveValue;
                bestAisles.clear();
                bestAisles.addAll(ws.selectedAisles.keySet());
                bestOrders.clear();
                bestOrders.addAll(ws.selectedOrders.keySet());
            }
        }
        return new ChallengeSolution(bestOrders, bestAisles);
    }
}
}
