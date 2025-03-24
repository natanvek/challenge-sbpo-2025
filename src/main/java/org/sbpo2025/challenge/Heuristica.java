package org.sbpo2025.challenge;

import java.util.*;

public abstract class Heuristica extends ChallengeSolver {
    private final long MAX_RUNTIME = 600000; // milliseconds; 10 minutes

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
    protected int os;
    protected int as;
    // protected int nItems;

    public int[] available_inicial;
    public Set<Integer> aisles_iniciales = new HashSet<>();

    protected class EfficientCart { // deberia crear una clase padre o mismo extender
        public int aisleCount(){
            return my_aisles.size();
        }

        public boolean hasAisle(Aisle a){
            return my_aisles.contains(a.id);
        }

        public Set<Integer> my_aisles = new HashSet<>();
        public int cantItems = 0;

        public Set<Integer> getAisles(){
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

        public EfficientCart(){}

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
                return as - 1;
            return Math.min((int) Math.floor(waveSizeUB / getValue()), as);
        }

        public boolean update(EfficientCart otro) {
            if (otro.getValue() > getValue()) {
                copy(otro);
                return true;
            }

            return false;
        }

        public void addAisle(Aisle a) {
            my_aisles.add(a.id);
        }

        public void addOrder(Order o) {
            cantItems += o.size;
        }


        public void setAvailable(){
            Inventory.reset();
            cantItems = 0;                               

            for(int a : my_aisles) 
                Inventory.addAisle(idToAisle[a]);
        }

        // asegurate de haber ejecutado resetAisles antes
        public void fill() { 
            setAvailable();
            for (Order o : orders) {
                if (cantItems + o.size > waveSizeUB)
                    continue;

                if (Inventory.checkAndRemove(o.items))
                    addOrder(o);
            }
        }

        // asegurate de llamarlo después de fill
        public void removeRedundantAisles() {
            for (Aisle p : aisles_sorted) 
                if (hasAisle(p) && Inventory.checkAndRemove(p.items))
                    my_aisles.remove(p.id);   
    
        }
    }

    public Heuristica(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int _nItems,
            int waveSizeLB, int waveSizeUB) {
        super(_orders, _aisles, _nItems, waveSizeLB, waveSizeUB);

        nItems = _nItems;
        available_inicial = new int[nItems];
        aisles = new Aisle[_aisles.size()];

                
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
            // boolean anda = true;
            for (Map.Entry<Integer, Integer> entry : _orders.get(o).entrySet()) {
                Integer elem = entry.getKey();
                Integer cant = entry.getValue();
                t += cant;
                // if(mapa_pasillos.getOrDefault(elem, 0) < cant )
                // anda = false;
            }
            // if(t <= waveSizeUB && anda)
            ordersh_aux.add(new Order(o, new HashMap<>(_orders.get(o)), t));
        }

        orders = ordersh_aux.toArray(new Order[0]);

        as = aisles.length;
        os = orders.length;

    }

    protected EfficientCart pasada(int tope) {
        EfficientCart rta = new EfficientCart();
        for (int sol = 0; sol < tope; ++sol) {
            EfficientCart actual = new EfficientCart();
            for (int p = 0; p <= sol; ++p)
                actual.addAisle(aisles[p]);

            actual.fill();

            actual.removeRedundantAisles();

            if (actual.cantItems >= waveSizeLB)
                rta.update(actual);

            tope = Math.min(tope, rta.getTope());

        }
        return rta;
    }


    public ChallengeSolution getSolution(EfficientCart s) {
        Set<Integer> rta_orders = new HashSet<>(), rta_aisles = new HashSet<>();

        s.setAvailable();

        for (Integer a : s.my_aisles)
            rta_aisles.add(a);

        for (Order o : orders)
            if (s.getCantItems() + o.size <= waveSizeUB && Inventory.checkAndRemove(o.items))
                rta_orders.add(o.id);

        return new ChallengeSolution(rta_orders, rta_aisles);
    }
}
