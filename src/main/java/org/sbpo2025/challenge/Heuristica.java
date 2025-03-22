package org.sbpo2025.challenge;
import java.util.*;

public abstract class Heuristica extends ChallengeSolver {
    protected static class Order {
        public int id;
        public Map<Integer, Integer> items;
        public int size;

        // Constructor para inicializar Order
        public Order(int _id, Map<Integer, Integer> _items, int _size) {
            this.id = _id;
            this.items = _items;
            this.size = _size;
        }
    }

    protected static class Aisle {
        public int id;
        public Map<Integer, Integer> items;
        public int size;

        // Constructor para inicializar Order
        public Aisle(int _id, Map<Integer, Integer> _items, int _size) {
            this.id = _id;
            this.items = _items;
            this.size = _size;
        }
    }

    protected Order[] orders;
    protected Aisle[] aisles;
    protected Aisle[] idToAisle;

    protected Aisle[] aisles_sorted;

    protected static int nItems;
    protected int nOrders;
    protected int nAisles;

    public int getTope(AisleCollection c) {
        if (c.getValue() < 1e-5)
            return nAisles;
        return Math.min((int) Math.floor(waveSizeUB / c.getValue()), nAisles);
    }

    public Heuristica(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int _nItems,
            int waveSizeLB, int waveSizeUB) {
        super(_orders, _aisles, _nItems, waveSizeLB, waveSizeUB);

        nItems = _nItems;
        AisleCollection.initializeInventory(nItems);
        aisles = new Aisle[_aisles.size()];
                
        for (int a = 0; a < _aisles.size(); a++) {
            aisles[a] = new Aisle(a, _aisles.get(a), 0);
            for (Map.Entry<Integer, Integer> entry : _aisles.get(a).entrySet())
                aisles[a].size += entry.getValue();
        }

        aisles_sorted = Arrays.copyOf(aisles, aisles.length);
        idToAisle = Arrays.copyOf(aisles, aisles.length);
        Arrays.sort(aisles_sorted, (a1, a2) -> Integer.compare(a1.size, a2.size));

        orders = new Order[_orders.size()];

        for (int o = 0; o < _orders.size(); o++) {
            orders[o] = new Order(o, _orders.get(o), 0);
            for (Map.Entry<Integer, Integer> entry : _orders.get(o).entrySet())
                orders[o].size += entry.getValue();
        }


        nAisles = aisles.length;
        nOrders = orders.length;

    }

    public void fill(AisleCollection s) {
        for (Order o : orders) {
            if (s.getCantItems() + o.size <= waveSizeUB && s.checkAndRemove(o.items)){
                s.addOrder(o);
            }

        }
    }

    public AisleCollection updateAnswer(AisleCollection rta, AisleCollection otro) {
        return rta.isWorseThan(otro) ? otro : rta;
    }

    public ChallengeSolution getSolution(AisleCollection s) {
        Set<Integer> rta_orders = new HashSet<>(), rta_aisles = new HashSet<>();
        
        s.setAvailable();

        for(Aisle a : s)
            rta_aisles.add(a.id);

        for (Order o : orders)
            if (s.getCantItems() + o.size <= waveSizeUB && s.checkAndRemove(o.items)) 
                rta_orders.add(o.id);

        return new ChallengeSolution(rta_orders, rta_aisles);
    }

}
