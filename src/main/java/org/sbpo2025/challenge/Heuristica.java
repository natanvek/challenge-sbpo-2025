package org.sbpo2025.challenge;

import org.apache.commons.lang3.time.StopWatch;
import org.sbpo2025.challenge.Heuristica.Cart;
import org.sbpo2025.challenge.Heuristica.EfficientCart;

import java.util.*;
import java.util.concurrent.TimeUnit;

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

    protected class Cart {
        public Set<Integer> my_orders = new HashSet<>();
        public Set<Integer> my_aisles = new HashSet<>();;
        public Map<Integer, Integer> available = new HashMap<>();
        
        public int cantItems = 0;

        public Cart() {}

        public void copy(Cart otro) {
            my_orders.clear();
            my_orders.addAll(otro.my_orders);
            my_aisles.clear();
            my_aisles.addAll(otro.my_aisles);
            available.clear();
            available.putAll(otro.available);
            cantItems = otro.cantItems;
        }

        public Cart(Cart otro) {
            copy(otro);
        }

        public double getValue() {
            if (my_aisles.size() == 0)
                return 0.0;
            return (double) cantItems / my_aisles.size();
        }

        public int getTope() {
            if (getValue() < 1e-5)
                return as;
            return Math.min((int) Math.floor(waveSizeUB / getValue()), as);
        }

        public boolean update(Cart otro) {
            if (otro.getValue() > getValue()) {
                copy(otro);
                return true;
            }

            return false;
        }

        public void addAisle(Aisle a) {
            my_aisles.add(a.id);
            for (Map.Entry<Integer, Integer> entry : a.items.entrySet())
                available.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        public void addOrder(Order o) {
            cantItems += o.size;
            my_orders.add(o.id);
        }


        public boolean removeRequestIfPossible(Map<Integer, Integer> m) {
            for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                if (available.getOrDefault(elem, 0).intValue() < cant)
                    return false;
            }
            for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                available.compute(elem, (k, tengo) -> tengo - cant);
            }
            return true;
        }


        public int aisleCount(){
            return my_aisles.size();
        }

        public boolean hasAisle(Aisle a){
            return my_aisles.contains(a.id);
        }

        public void resetOrders(){
            my_orders.clear();
            cantItems = 0;
            for(int a : my_aisles) 
                addAisle(idToAisle[a]);
        }

        public void fill() {            
            for (Order o : orders) {
                if (cantItems + o.size > waveSizeUB)
                    continue;

                if (removeRequestIfPossible(o.items))
                    addOrder(o);
            }
        }

        public void removeRedundantAisles() {
            for (Aisle p : aisles_sorted) 
                if (my_aisles.contains(p.id) && removeRequestIfPossible(p.items))
                    my_aisles.remove(p.id);   
    
        }
    }

    public long medirFill = 0;
    public long medirSetAvailable = 0;
    public long medirCopy = 0;

    protected class EfficientCart { // deberia crear una clase padre o mismo extender
        private static int[] available = new int[nItems];
        private static int[] modifiedDate = new int[nItems];
        private static int currentDate = 1;

        public Set<Integer> my_aisles = new HashSet<>();
        public int cantItems = 0;

        public Set<Integer> getAisles(){
            return my_aisles;
        }

        public void copy(EfficientCart otro) {
            medirCopy += my_aisles.size() * 2;
            medirCopy += otro.my_aisles.size() * 2;
            my_aisles.clear();
            my_aisles.addAll(otro.my_aisles);
            cantItems = otro.cantItems;
        }

        public EfficientCart(EfficientCart otro) {
            copy(otro);
        }

        public EfficientCart(){}

        public double getValue() {
            if (my_aisles.size() == 0)
                return 0.0;
            return (double) cantItems / my_aisles.size();
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

        public int aisleCount(){
            return my_aisles.size();
        }

        public boolean hasAisle(Aisle a){
            return my_aisles.contains(a.id);
        }

        private void addToAvailable(Aisle a) {
            for (Map.Entry<Integer, Integer> entry : a.items.entrySet()){
                medirSetAvailable++;
                if(modifiedDate[entry.getKey()] < currentDate)
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

        public boolean removeRequestIfPossible(Map<Integer, Integer> m) {
            for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                ++medirFill;
                if (modifiedDate[elem] < currentDate || available[elem] < cant)
                    return false;
            }
            for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                available[elem] -= cant;
                ++medirFill;
            }

            return true;
        }

        public void setAvailable(){
            currentDate++;
            cantItems = 0;                
            for(int a : my_aisles) 
                addToAvailable(idToAisle[a]);

        }

        // asegurate de haber ejecutado resetAisles antes
        public void fill() { 
            setAvailable();
            for (Order o : orders) {
                if (cantItems + o.size > waveSizeUB)
                    continue;

                if (removeRequestIfPossible(o.items))
                    addOrder(o);
            }
        }

        // asegurate de llamarlo después de fill
        public void removeRedundantAisles() {
            for (Aisle p : aisles_sorted) 
                if (hasAisle(p) && removeRequestIfPossible(p.items))
                    my_aisles.remove(p.id);   
    
        }
    }
    protected class EfficientCart2 { // deberia crear una clase padre o mismo extender
        private static int[] available = new int[nItems];
        private static ArrayList<Integer> visited = new ArrayList<>();
        
        EfficientCart2 padre;
        public int cantItems = 0;
        public int pasillos;
        public Aisle myAisle;
        private boolean availablePropio;

        Map<Integer, Integer> available2 = new HashMap<>();

        public void copy(EfficientCart2 otro) {
            cantItems = otro.cantItems;
            pasillos = otro.pasillos;
            available2.clear(); available2.putAll(otro.available2);
            padre = otro.padre;
            availablePropio = otro.availablePropio;
            myAisle = otro.myAisle;
            
        }
        public EfficientCart2(){
            availablePropio = true;
            pasillos = 0;
        }

        public EfficientCart2(EfficientCart2 otro) {
            otro.getAvailablePropio();
            padre = otro;
            pasillos = padre.pasillos;
            availablePropio = false;
        }

        public void getAvailablePropio() {
            if(availablePropio) return;
            available2.clear(); available2.putAll(padre.available2);
            for (Map.Entry<Integer, Integer> entry : myAisle.items.entrySet())
                available2.merge(entry.getKey(), entry.getValue(), Integer::sum);

            availablePropio = true;
        }

        public double getValue() {
            if (pasillos == 0)
                return 0.0;
            return (double) cantItems / pasillos;
        }

        public int getCantItems() {
            return cantItems;
        }

        public int getTope() {
            if (getValue() < 1e-5)
                return as - 1;
            return Math.min((int) Math.floor(waveSizeUB / getValue()), as);
        }

        public boolean update(EfficientCart2 otro) {
            if (otro.getValue() > getValue()) {
                copy(otro);
                return true;
            }

            return false;
        }

        public int aisleCount(){
            return pasillos;
        }

        // public boolean hasAisle(Aisle a){
        //     return my_aisles.contains(a.id);
        // }


        public Set<Integer> getAisles(){
            Set<Integer> my_aisles = new HashSet<>();

            EfficientCart2 estoy = this;
            while(estoy != null && estoy.myAisle != null) {
                my_aisles.add(estoy.myAisle.id);
                estoy = estoy.padre;
            }
            return my_aisles;
        }

         // asumo que el pasillo a agregar no está
         // asumo que lo ejecutas una sola vez, se puede hacer un new_aisles en caso de ser necesario
        public void addAisle(Aisle a) {
            if(myAisle != null)
                throw new Error("Aisle already added");

            myAisle = a;
            ++pasillos;
            for (Map.Entry<Integer, Integer> entry : a.items.entrySet())
                available2.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        public void addOrder(Order o) {
            cantItems += o.size;
        }

        public void addOrder(int posInOrdersh) {
            addOrder(orders[posInOrdersh]);
        }

        /* 
        public boolean removeRequestIfPossible(Map<Integer, Integer> m) {
            if (m.size() > available2.size()) 
                return false;
        
            Iterator<Map.Entry<Integer, Integer>> itM = m.entrySet().iterator();
            Iterator<Map.Entry<Integer, Integer>> itAv = available2.entrySet().iterator();
        
            Map.Entry<Integer, Integer> entryM = itM.hasNext() ? itM.next() : null;
            Map.Entry<Integer, Integer> entryAv = itAv.hasNext() ? itAv.next() : null;
        
            // Primera pasada: verificar si se puede restar
            while (entryM != null && entryAv != null) {
                int keyM = entryM.getKey(), valueM = entryM.getValue();
                int keyAv = entryAv.getKey(), valueAv = entryAv.getValue();
        
                if (keyM == keyAv) {
                    if (valueAv < valueM) return false; // No hay suficiente cantidad
                    entryM = itM.hasNext() ? itM.next() : null;
                    entryAv = itAv.hasNext() ? itAv.next() : null;
                } else if (keyM > keyAv) {
                    entryAv = itAv.hasNext() ? itAv.next() : null; // Avanzar en available2
                } else {
                    return false; // Si keyM < keyAv, significa que falta un elemento
                }
            }
            if (entryM != null) return false; // Si quedan elementos en m, no está contenido
        
            // Segunda pasada: restar los valores
            itM = m.entrySet().iterator();
            itAv = available2.entrySet().iterator();
            entryM = itM.hasNext() ? itM.next() : null;
            entryAv = itAv.hasNext() ? itAv.next() : null;
        
            while (entryM != null && entryAv != null) {
                int keyM = entryM.getKey(), valueM = entryM.getValue();
                int keyAv = entryAv.getKey(), valueAv = entryAv.getValue();
        
                if (keyM == keyAv) {
                    available2.put(keyAv, valueAv - valueM);
                    entryM = itM.hasNext() ? itM.next() : null;
                    entryAv = itAv.hasNext() ? itAv.next() : null;
                } else {
                    entryAv = itAv.hasNext() ? itAv.next() : null;
                }
            }
            return true;
        }
        */

        public boolean removeRequestIfPossible(Map<Integer, Integer> m) {
            if(m.size() > visited.size())
                return false;

            for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                if (available[elem] < cant)
                    return false;
            }
            for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                available[elem] -= cant;
            }

            return true;
        }

        private void resetAvailable(boolean esParaMi){

            if(availablePropio) {
                if(esParaMi) cantItems = 0;
    
                for(int k : visited)
                    available[k] = 0;
    
                visited.clear();
            } else {
                if(padre != null)
                    padre.resetAvailable(false);
                cantItems = 0;
            }

            for (Map.Entry<Integer, Integer> entry : available2.entrySet()) {
                if(available[entry.getKey()] == 0)
                    visited.add(entry.getKey());
                
                available[entry.getKey()] += entry.getValue();
            }

        }

        public void resetAvailable() {
            resetAvailable(true); // Valor por defecto
        }

        // asegurate de haber ejecutado resetAisles antes
        public void fill() { 
                       
            for (Order o : orders) {
                if (cantItems + o.size > waveSizeUB)
                    continue;

                if (removeRequestIfPossible(o.items))
                    addOrder(o);
            }
        }

        // // asegurate de llamarlo después de fill
        // public void removeRedundantAisles() {
        //     for (Aisle p : aisles_sorted) 
        //         if (hasAisle(p) && removeRequestIfPossible(p.items))
        //             my_aisles.remove(p.id);   
    
        // }
    }

    public Heuristica(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int _nItems,
            int waveSizeLB, int waveSizeUB) {
        super(_orders, _aisles, _nItems, waveSizeLB, waveSizeUB);

        nItems = _nItems;
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

    protected Cart pasada(int tope) {
        Cart rta = new Cart();
        for (int sol = 0; sol < tope; ++sol) {
            Cart actual = new Cart();
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

    /*
     * Get the remaining time in seconds
     */
    protected long getRemainingTime(StopWatch stopWatch) {
        return Math.max(
                TimeUnit.SECONDS.convert(MAX_RUNTIME - stopWatch.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
                0);
    }

    protected boolean isSolutionFeasible(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return false;
        }

        int[] totalUnitsPicked = new int[nItems];
        int[] totalUnitsAvailable = new int[nItems];

        // Calculate total units picked
        for (int order : selectedOrders) {
            for (Map.Entry<Integer, Integer> entry : orders[order].items.entrySet()) {
                totalUnitsPicked[entry.getKey()] += entry.getValue();
            }
        }

        // Calculate total units available
        for (int aisle : visitedAisles) {
            for (Map.Entry<Integer, Integer> entry : aisles[aisle].items.entrySet()) {
                totalUnitsAvailable[entry.getKey()] += entry.getValue();
            }
        }

        // Check if the total units picked are within bounds
        int totalUnits = Arrays.stream(totalUnitsPicked).sum();
        if (totalUnits < waveSizeLB || totalUnits > waveSizeUB) {
            return false;
        }

        // Check if the units picked do not exceed the units available
        for (int i = 0; i < nItems; i++) {
            if (totalUnitsPicked[i] > totalUnitsAvailable[i]) {
                return false;
            }
        }

        return true;
    }

    protected double computeObjectiveFunction(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return 0.0;
        }
        int totalUnitsPicked = 0;

        // Calculate total units picked
        for (int order : selectedOrders) {
            totalUnitsPicked += orders[order].items.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        // Calculate the number of visited aisles
        int numVisitedAisles = visitedAisles.size();

        // Objective function: total units picked / number of visited aisles
        if (numVisitedAisles == 0)
            return 0;

        return (double) totalUnitsPicked / numVisitedAisles;
    }
}
