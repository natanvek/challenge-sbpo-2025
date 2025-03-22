package org.sbpo2025.challenge;

import org.sbpo2025.challenge.Heuristica.Aisle;
import java.util.*;

public class Inventory {
    // Arrays estáticos compartidos por todas las instancias de la clase
    private static int[] availableAux; // Array para los items disponibles
    private static int[] modifiedDate; // Array para las fechas de modificación
    private static int currentDate = 1; // Fecha global para todos

    // private Map<Integer, Integer> available;

    public static void initializeStaticVar(int nItems) {
        availableAux = new int[nItems]; // Inicializa el array solo una vez
        modifiedDate = new int[nItems]; // Inicializa el array de fechas solo una vez
    }

    // Constructor
    Inventory() {
        // available = new HashMap<>();
    }

    public void copy(Inventory otro) {
        // available.clear(); available.putAll(otro.available);
        ++currentDate;
    }

    private void set(int elem, int amount) {
        availableAux[elem] = amount; // Actualiza la cantidad disponible
        modifiedDate[elem] = currentDate; // Actualiza la fecha de modificación
    }

    private int get(int elem) {
        if (modifiedDate[elem] < currentDate)
            return 0;

        // set(elem, available.getOrDefault(elem, 0));

        return availableAux[elem];
    }

    public void reset() {
        ++currentDate;

        // for (Map.Entry<Integer, Integer> entry : available.entrySet())
        // set(entry.getKey(), get(entry.getKey()) + entry.getValue());

    }

    public boolean checkAndRemove(Map<Integer, Integer> items) {
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            int elem = entry.getKey(), cant = entry.getValue();
            if (get(elem) < cant)
                return false;
        }
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            int elem = entry.getKey(), cant = entry.getValue();
            availableAux[elem] -= cant;
        }
        return true;
    }

    // protected void addAisle(Aisle a) {
    // a.items.forEach((key, value) -> available.merge(key, value, Integer::sum));
    // }
    protected void addAisle2(Aisle a) {

        for (Map.Entry<Integer, Integer> entry : a.items.entrySet()) {
            int elem = entry.getKey(), cant = entry.getValue();
            set(elem, get(elem) + cant);
        }

    }
}