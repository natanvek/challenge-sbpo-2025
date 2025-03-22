package org.sbpo2025.challenge;

import org.sbpo2025.challenge.Heuristica.Aisle;
import org.sbpo2025.challenge.Heuristica.Order;

import java.util.*;

public abstract class AisleCollection implements Iterable<Aisle> {

    public int cantItems = 0;
    public int capacity = 0;
    protected static Inventory inv = new Inventory();

    public static void initializeInventory(int nItems) {
        Inventory.initializeStaticVar(nItems);
    }
    
    abstract int aisleCount();

    public double getValue() {
        if (aisleCount() == 0)
            return 0.0;

        return (double) cantItems / aisleCount();
    }

    public boolean isWorseThan(AisleCollection otro) {
        return getValue() < otro.getValue();
    }

    abstract void addAisle(Aisle a);

    public void addOrder(Order o) {
        cantItems += o.size;
    }

    abstract boolean hasAisle(Aisle a);

    abstract Set<Aisle> getAisles();

    public int getCantItems() {
        return cantItems;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean checkAndRemove(Map<Integer, Integer> items) {
        return inv.checkAndRemove(items);
    }

    public void setAvailable() {
        inv.reset();
        for(Aisle a : this) 
            inv.addAisle2(a);

        cantItems = 0;
    }
    
    public abstract Iterator<Aisle> iterator();

}
