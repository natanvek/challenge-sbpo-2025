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

    public <AC extends AisleCollection> int getTope(AC c) {
        if (c.getValue() < 1e-5)
            return nAisles;
        return Math.min((int) Math.floor(waveSizeUB / c.getValue()), nAisles);
    }

    public int getTope(EfficientCart c) {
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

    protected class EfficientCart { // deberia crear una clase padre o mismo extender
        private static int[] available = new int[nItems];
        private static int[] modifiedDate = new int[nItems];
        private static int currentDate = 1;

        public int aisleCount() {
            return my_aisles.size();
        }

        public boolean hasAisle(Aisle a) {
            return my_aisles.contains(a.id);
        }

        public Set<Integer> my_aisles = new HashSet<>();
        public int cantItems = 0;

        public Set<Integer> getAisles() {
            return my_aisles;
        }

        public void copy(EfficientCart otro) {
            my_aisles.clear();
            my_aisles.addAll(otro.my_aisles);
            cantItems = otro.cantItems;
        }

        public EfficientCart(EfficientCart otro) {
            copy(otro);
        }

        public EfficientCart() {
        }

        public double getValue() {
            if (aisleCount() == 0)
                return 0.0;
            return (double) cantItems / (aisleCount());
        }

        public int getCantItems() {
            return cantItems;
        }

        public boolean isWorseThan(EfficientCart otro) {
            return getValue() < otro.getValue();
        }

        public int getTope() {
            if (getValue() < 1e-5)
                return nAisles;
            return Math.min((int) Math.floor(waveSizeUB / getValue()), nAisles);
        }

        public boolean update(EfficientCart otro) {
            if (otro.getValue() > getValue()) {
                copy(otro);
                return true;
            }

            return false;
        }

        private void addToAvailable(Aisle a) {
            for (Map.Entry<Integer, Integer> entry : a.items.entrySet()) {
                if (modifiedDate[entry.getKey()] < currentDate)
                    available[entry.getKey()] = 0;

                modifiedDate[entry.getKey()] = currentDate;
                available[entry.getKey()] += entry.getValue();
            }
        }

        public void addAisle(Aisle a) {
            my_aisles.add(a.id);
        }

        public void addOrder(Order o) {
            cantItems += o.size;
        }

        public boolean checkAndRemove(Map<Integer, Integer> m) {
            for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                if (modifiedDate[elem] < currentDate) {
                    modifiedDate[elem] = currentDate;
                    available[elem] = 0;
                }

                if (available[elem] < cant)
                    return false;
            }
            for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                available[elem] -= cant;
            }

            return true;
        }

        public void setAvailable() {
            currentDate++;
            cantItems = 0;

            for (int a : my_aisles)
                addToAvailable(idToAisle[a]);
        }

        // asegurate de haber ejecutado resetAisles antes
        public void fill() {
            setAvailable();
            for (Order o : orders) {
                if (cantItems + o.size > waveSizeUB)
                    continue;

                if (checkAndRemove(o.items))
                    addOrder(o);
            }
        }

        // asegurate de llamarlo después de fill
        public void removeRedundantAisles() {
            for (Aisle p : aisles_sorted)
                if (hasAisle(p) && checkAndRemove(p.items))
                    my_aisles.remove(p.id);

        }
    }

    public void fill(AisleSet s) {
        for (Order o : orders) {
            if (s.getCantItems() + o.size <= waveSizeUB && s.checkAndRemove(o.items)) {
                s.addOrder(o);
            }

        }
    }

    public void fill(EfficientCart s) {
        for (Order o : orders) {
            if (s.getCantItems() + o.size <= waveSizeUB && s.checkAndRemove(o.items)) {
                s.addOrder(o);
            }

        }
    }

    public static <AC extends AisleCollection> AC updateAnswer(AC rta, AC otro) {
        return rta.isWorseThan(otro) ? otro : rta;
    }

    public EfficientCart updateAnswer(EfficientCart rta, EfficientCart otro) {
        return rta.isWorseThan(otro) ? otro : rta;
    }

    public ChallengeSolution getSolution(EfficientCart s) {
        Set<Integer> rta_orders = new HashSet<>(), rta_aisles = new HashSet<>();

        s.setAvailable();

        for (Integer a : s.my_aisles)
            rta_aisles.add(a);

        for (Order o : orders)
            if (s.getCantItems() + o.size <= waveSizeUB && s.checkAndRemove(o.items))
                rta_orders.add(o.id);

        return new ChallengeSolution(rta_orders, rta_aisles);
    }

    public ChallengeSolution getSolution(AisleCollection s) {
        Set<Integer> rta_orders = new HashSet<>(), rta_aisles = new HashSet<>();

        s.setAvailable();

        for (Aisle a : s)
            rta_aisles.add(a.id);

        for (Order o : orders)
            if (s.getCantItems() + o.size <= waveSizeUB && s.checkAndRemove(o.items))
                rta_orders.add(o.id);

        return new ChallengeSolution(rta_orders, rta_aisles);
    }

}
