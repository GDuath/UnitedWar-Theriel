package org.unitedlands.war.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.unitedlands.war.UnitedWar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class WarBookData {
    private UUID attackerTownId;
    private String attackerTownName;
    private UUID targetTownId;
    private String targetTownName;
    private WarGoal warGoal;

    private ItemStack warBook;

    public WarBookData(UUID attackerTownId, String attackerTownName, UUID targetTownId, String targetTownName,
            WarGoal warGoal) {
        this.attackerTownId = attackerTownId;
        this.attackerTownName = attackerTownName;
        this.targetTownId = targetTownId;
        this.targetTownName = targetTownName;
        this.warGoal = warGoal;

        this.warBook = new ItemStack(Material.WRITABLE_BOOK);
    }

    public WarBookData(ItemStack book) {
        this.warBook = book;

        try {
            this.attackerTownId = UUID.fromString(getItemMeta("book.attackerId"));
            this.attackerTownName = getItemMeta("book.attackerName");
            this.targetTownId = UUID.fromString(getItemMeta("book.targetId"));
            this.targetTownName = getItemMeta("book.targetName");
            this.warGoal = WarGoal.valueOf(getItemMeta("book.goal"));
        } catch (Exception ignored) {

        }
    }

    public UUID getAttackerTownId() {
        return attackerTownId;
    }

    public String getAttackerTownName() {
        return attackerTownName;
    }

    public UUID getTargetTownId() {
        return targetTownId;
    }

    public String getTargetTownName() {
        return targetTownName;
    }

    public WarGoal getWarGoal() {
        return warGoal;
    }

    public ItemStack getBook() {
        addPages();
        addItemMeta();
        attachWarData();
        return warBook;
    }

    private void addPages() {
        var bookMeta = (BookMeta) warBook.getItemMeta();
        var bookContent = UnitedWar.getInstance().getConfig().getString("messages.war-book-content", "");
        bookMeta.addPages(Component.text(bookContent));
        warBook.setItemMeta(bookMeta);
    }

    private void addItemMeta() {
        ItemMeta meta = warBook.getItemMeta();
        meta.addEnchant(Enchantment.LURE, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        Component name = Component
                .text(UnitedWar.getInstance().getConfig().getString("messages.war-book-name") + targetTownName);

        meta.displayName(name);
        meta.lore(getLore());
        //meta.setCustomModelData(2);
        warBook.setItemMeta(meta);
    }

    private String getItemMeta(String keyName) {
        ItemMeta meta = warBook.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        try {
            return pdc.get(getKey(keyName), PersistentDataType.STRING);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<Component> getLore() {
        List<String> configuredLore = UnitedWar.getInstance().getConfig().getStringList("messages.war-book-lore");
        List<Component> componentLore = new ArrayList<>(configuredLore.size());
        for (String line : configuredLore) {
            Component component = Component.text(line);
            componentLore.add(component);
        }
        return componentLore;
    }

    private void attachWarData() {
        ItemMeta meta = warBook.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(getKey("book.attackerId"), PersistentDataType.STRING, attackerTownId.toString());
        pdc.set(getKey("book.attackerName"), PersistentDataType.STRING, attackerTownName);
        pdc.set(getKey("book.targetId"), PersistentDataType.STRING, targetTownId.toString());
        pdc.set(getKey("book.targetName"), PersistentDataType.STRING, targetTownName);
        pdc.set(getKey("book.goal"), PersistentDataType.STRING, warGoal.toString());
        pdc.set(getKey("book.warbook"), PersistentDataType.INTEGER, 1);

        warBook.setItemMeta(meta);
    }

    private NamespacedKey getKey(String name) {
        return new NamespacedKey(UnitedWar.getInstance(), name);
    }

    public boolean isWarBook() {
        ItemMeta meta = warBook.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        try {
            return pdc.get(getKey("book.warbook"), PersistentDataType.INTEGER) == 1;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getWarName() {
        ItemMeta meta = warBook.getItemMeta();
        return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
    }

    public String getWarDescription() {
        var bookMeta = (BookMeta) warBook.getItemMeta();

        var description = "";
        var pages = bookMeta.pages();
        for (var page : pages) {
            description += PlainTextComponentSerializer.plainText().serialize(page) + " ";
        }

        return description.toString()
                .replace("\r", " ") 
                .replace("\n", " ") 
                .replaceAll("\\s+", " ") 
                .trim(); 
    }
}
