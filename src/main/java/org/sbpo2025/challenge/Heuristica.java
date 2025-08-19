package org.sbpo2025.challenge;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

public abstract class Heuristica extends ChallengeSolver {

    protected static class Order {
        public int id;
        public Map<Integer, Integer> items;
        public int size;
        public int pos;

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
        public int pos;

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

    protected int nOrders;
    protected int nAisles;
    protected int nItems;
    protected int tope;
    protected EfficientCart rta;

    public int[] available_inicial;
    public Set<Integer> aisles_iniciales = new HashSet<>();

    public Heuristica(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int _nItems,
            int waveSizeLB, int waveSizeUB) {
        super(_orders, _aisles, _nItems, waveSizeLB, waveSizeUB);

        available_inicial = new int[nItems];
        aisles = new Aisle[_aisles.size()];
        nItems = _nItems;

        Inventory.initializeStaticVar(nItems);

        for (int a = 0; a < _aisles.size(); a++) {
            aisles[a] = new Aisle(a, new HashMap<>(_aisles.get(a)), 0);
            for (Map.Entry<Integer, Integer> entry : _aisles.get(a).entrySet())
                aisles[a].size += entry.getValue();
        }

        aisles_sorted = Arrays.copyOf(aisles, aisles.length);
        idToAisle = Arrays.copyOf(aisles, aisles.length);
        Arrays.sort(aisles_sorted, (a1, a2) -> Integer.compare(a1.size, a2.size));

        List<Order> ordersh_aux = new ArrayList<>();

        for (int o = 0; o < _orders.size(); o++) {
            int t = 0;
            double[] sabotea = new double[nItems];
            boolean anda = true;
            for (Map.Entry<Integer, Integer> entry : _orders.get(o).entrySet()) {
                Integer elem = entry.getKey();
                Integer cant = entry.getValue();
                t += cant;
                sabotea[elem] = 0;
                int hay = 0;
                for (Aisle a : aisles)
                    hay += Math.min(a.items.getOrDefault(elem, 0), cant);

                if (hay < cant)
                    anda = false;

                sabotea[elem] = (double) hay / cant;
            }

            // Collections.sort(o_items, (i1, i2) -> Double.compare(sabotea[i1.getKey()],
            // sabotea[i2.getKey()]));

            // if(t <= waveSizeUB && anda) ojo con esto que me descuajeringa el id
            ordersh_aux.add(new Order(o, _orders.get(o), t));

        }

        orders = ordersh_aux.toArray(new Order[0]);

        nAisles = aisles.length;
        tope = nAisles;
        nOrders = orders.length;

        rta = new EfficientCart();
    }

    protected class EfficientCart { // deberia crear una clase padre o mismo extender

        public Set<Aisle> my_aisles = new HashSet<>();
        public int cantItems = 0;

        public int aisleCount() {
            return my_aisles.size();
        }

        public boolean hasAisle(Aisle a) {
            return my_aisles.contains(a);
        }

        public Set<Aisle> getAisles() {
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
            if (aisleCount() + aisles_iniciales.size() == 0)
                return 0.0;
            return (double) cantItems / (aisleCount() + aisles_iniciales.size());
        }

        public int getCantItems() {
            return cantItems;
        }

        public int getTope() {
            if (getValue() < 1e-5)
                return nAisles - 1;
            return Math.min((int) Math.floor(waveSizeUB / getValue()), nAisles);
        }

        public boolean update(EfficientCart otro) {
            if (getValue() >= otro.getValue())
                return false;

            cantItems = otro.cantItems;
            my_aisles = otro.my_aisles;
            return true;
        }

        public void addAisle(Aisle a) {
            my_aisles.add(a);
        }

        public void addOrder(Order o) {
            cantItems += o.size;
        }

        public void setAvailable() {
            Inventory.reset();
            cantItems = 0;

            for (Aisle a : my_aisles)
                Inventory.addAisle(a);
        }

        public Set<Order> getOrders() {
            Set<Order> rta_orders = new HashSet<>();

            setAvailable();

            for (Order o : orders)
                if (getCantItems() + o.size <= waveSizeUB && Inventory.checkAndRemove(o.items)) {
                    rta_orders.add(o);
                    addOrder(o);
                }

            return rta_orders;
        }

        // asegurate de haber ejecutado resetAisles antes

        // asegurate de llamarlo después de fill
        public void removeRedundantAisles() {
            for (Aisle p : aisles_sorted)
                if (hasAisle(p) && Inventory.checkAndRemove(p.items))
                    my_aisles.remove(p);

        }
    }

    public void updateRta(EfficientCart ec) {
        EfficientCart copy = new EfficientCart(ec);
        copy.removeRedundantAisles();
        if (ec.cantItems >= waveSizeLB) {
            rta.update(copy);
            tope = Math.min(tope, rta.getTope());
        }
    }

    public void fill(EfficientCart ec) {
        ec.setAvailable();
        for (Order o : orders)
            if (ec.getCantItems() + o.size <= waveSizeUB && Inventory.checkAndRemove(o.items))
                ec.addOrder(o);
    }

    protected void pasada() {
        EfficientCart actual = new EfficientCart();
        for (int sol = 0; sol < tope; ++sol) {
            actual.addAisle(aisles[sol]);
            fill(actual);
            updateRta(actual);
        }
    }

    public void printElapsedTime(StopWatch stopWatch) {
        long elapsedMillis = stopWatch.getNanoTime() / 1_000_000;
        long hours = elapsedMillis / (1000 * 60 * 60);
        long minutes = (elapsedMillis / (1000 * 60)) % 60;
        long seconds = (elapsedMillis / 1000) % 60;
        System.out.println(String.format("Tiempo transcurrido: %02d:%02d:%02d", hours, minutes, seconds));
    }

    public int calcRegisterSize(double minutos) {
        StopWatch stopWatch = StopWatch.createStarted();
        long ti = stopWatch.getNanoTime(), tiempo_iterando = 0, iteraciones = 0;
        while (tiempo_iterando < 5e9) {
            for (int i = 0; i < 100; ++i) {
                EfficientCart simulatingBest = new EfficientCart();

                List<Aisle> perm = new ArrayList<>();
                for (Aisle a : aisles)
                    perm.add(a);
                Collections.shuffle(perm);

                for (int r = 0; r < tope; ++r) {
                    EfficientCart estimatingRegisterSize = new EfficientCart(simulatingBest);
                    estimatingRegisterSize.addAisle(perm.get(r));
                    fill(estimatingRegisterSize);
                    simulatingBest = estimatingRegisterSize;
                }
            }
            tiempo_iterando = (stopWatch.getNanoTime() - ti);
            iteraciones += 100;
        }
        double tope_por_fill = (double) tiempo_iterando / (iteraciones * 1e6);
        return Math.max(1, Math.min(1000, (int) ((minutos * 1000.0 * 60.0) / (tope_por_fill * nAisles))));
    }

    public ChallengeSolution getSolution() {
        Set<Integer> rta_orders = new HashSet<>(), rta_aisles = new HashSet<>();

        rta.setAvailable();

        for (Aisle a : rta.my_aisles)
            rta_aisles.add(a.id);

        for (Order o : orders)
            if (rta.getCantItems() + o.size <= waveSizeUB && Inventory.checkAndRemove(o.items)) {
                rta_orders.add(o.id);
                rta.addOrder(o);
            }

        return new ChallengeSolution(rta_orders, rta_aisles);
    }
}
