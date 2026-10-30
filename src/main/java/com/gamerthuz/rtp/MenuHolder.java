package com.gamerthuz.rtp;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class MenuHolder implements InventoryHolder {
    private Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void set(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);
        if (handler != null) handlers.put(slot, handler);
        else handlers.remove(slot);
    }

    public void click(InventoryClickEvent e) {
        Consumer<InventoryClickEvent> h = handlers.get(e.getSlot());
        if (h != null) h.accept(e);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
