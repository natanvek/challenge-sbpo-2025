package org.sbpo2025.challenge;

import org.sbpo2025.challenge.Heuristica.Aisle;

import java.util.*;

public class AisleSet extends AisleCollection {

    protected Set<Aisle> aisles;


    public AisleSet() {
        aisles = new HashSet<>();
    }

    public void copy(AisleSet otro) {
        aisles.clear();
        aisles.addAll(otro.aisles);
        cantItems = otro.cantItems;
        capacity = otro.capacity;
    }

    public AisleSet(AisleSet otro) {
        aisles = new HashSet<>(otro.aisles);
    }

    public int aisleCount() {
        return aisles.size();
    };


    public void addAisle(Aisle a) {
        aisles.add(a);
        capacity += a.size;
    };


    public boolean hasAisle(Aisle a) {
        return aisles.contains(a);
    }

    public Set<Aisle> getAisles() {
        return aisles;
    };

    @Override
    public Iterator<Aisle> iterator() {
        return aisles.iterator();
    }

}
