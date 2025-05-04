package org.unitedlands.managers;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.warevents.BaseWarEvent;
import org.unitedlands.classes.warevents.SampleEvent;
import org.unitedlands.models.WarEventRecord;
import org.bukkit.event.HandlerList;

public class WarEventManager {

    private final UnitedWar plugin;

    private Map<String, BaseWarEvent> eventRegister = new HashMap<>();
    private Map<String, Double> eventChances = new HashMap<>();

    private BaseWarEvent currentEvent = null;
    private WarEventRecord currentEventRecord = null;
    private Long currentEventStartTime = null;

    private @Nullable Integer lastCheckedHour = null;

    public WarEventManager(UnitedWar plugin) {
        this.plugin = plugin;
        buildEventRegister();
    }

    private void buildEventRegister() {
        eventRegister.put("SAMPLE1", new SampleEvent("SAMPLE1", "Sample event 1", "Lorem ipsum dolor sit amet.", 120L));
        eventRegister.put("SAMPLE2", new SampleEvent("SAMPLE2", "Sample event 2", "Lorem ipsum dolor sit amet.", 120L));
        eventRegister.put("SAMPLE3", new SampleEvent("SAMPLE3", "Sample event 3", "Lorem ipsum dolor sit amet.", 120L));
        eventRegister.put("SAMPLE4", new SampleEvent("SAMPLE4", "Sample event 4", "Lorem ipsum dolor sit amet.", 120L));

        var pickTable = plugin.getConfig().getConfigurationSection("war-events.pick-table");
        for (String key : pickTable.getKeys(false)) {
            eventChances.put(key, pickTable.getDouble(key, 0.5));
        }

    }

    public void loadEventRecord() {
        var warEvenRecordDbService = plugin.getDatabaseManager().getWarEventRecordDbService();
        warEvenRecordDbService.getIncompleteAsync().thenAccept(record -> {
            if (record != null) {
                currentEventRecord = record;
                currentEventStartTime = record.getStart_time();
                currentEvent = eventRegister.get(record.getEvent_type());
                if (currentEvent != null) {
                    plugin.getLogger().info("Loaded ongoing event: " + currentEvent.getDisplayname());
                } else {
                    plugin.getLogger().severe("No event found for the loaded ongoing record. Event type: " + record.getEvent_type());
                }
            }
        });
    }

    public void handleEvents() {
        handleCurrentEvent();
        if (currentEvent == null) {
            handleEventCreation();
        }
    }

    public Boolean isEventActive() {
        return currentEvent != null;
    }

    public void resetEvent() {
        if (currentEvent != null) {
            removeCurrentEvent();
        }
        lastCheckedHour = null; // Reset the last checked hour
        plugin.getLogger().info("Event reset.");
    }


    private void handleCurrentEvent() {
        if (currentEvent == null) {
            return;
        }

        plugin.getLogger().info("Active event: " + currentEvent.getDisplayname());

        var currentTime = System.currentTimeMillis();
        var eventDuration = currentEvent.getDuration() * 1000L; // Convert to milliseconds
        var eventEndTime = currentEventStartTime + eventDuration;
        if (currentTime >= eventEndTime) {
            removeCurrentEvent();
        }
    }

    private void handleEventCreation() {

        // There should only be one attempts at creating an event per hour

        long currentTimeMillis = System.currentTimeMillis();
        int currentHour = (int) (currentTimeMillis / (1000 * 60 * 60)) % 24; // Get the current hour (0-23)


        if (lastCheckedHour == null || currentHour != lastCheckedHour) {
            lastCheckedHour = currentHour;
            double eventChance = plugin.getConfig().getDouble("war-events.random-event-chance", 0.5f);
            if (Math.random() < eventChance) {
                pickRandomEvent();
            }
        }
        else {
            plugin.getLogger().info("Event creation skipped this hour.");
            plugin.getLogger().info("Current hour: " + currentHour);
            plugin.getLogger().info("Last checked hour: " + lastCheckedHour);
            
        }
    }

    public void pickRandomEvent() {
        currentEvent = doWeightedRandomSelection();
        if (currentEvent != null) {
            currentEventStartTime = System.currentTimeMillis();
            Bukkit.getPluginManager().registerEvents((Listener) currentEvent, plugin);

            currentEventRecord = new WarEventRecord();
            currentEventRecord.setTimestamp(System.currentTimeMillis());
            currentEventRecord.setStart_time(System.currentTimeMillis());
            currentEventRecord.setScheduled_end_time(System.currentTimeMillis() + (currentEvent.getDuration() * 1000L)); 
            currentEventRecord.setEvent_type(currentEvent.getInternalName());

            var warEvenRecordDbService = plugin.getDatabaseManager().getWarEventRecordDbService();
            warEvenRecordDbService.createOrUpdateAsync(currentEventRecord);
        } else {
            lastCheckedHour = null; // Reset the last checked hour if no event is picked
            plugin.getLogger().warning("No event was picked. Please check the event register.");
        }
    }

    private BaseWarEvent doWeightedRandomSelection() {
        double totalWeight = 0;
        for (var e : eventChances.entrySet()) {
            totalWeight += e.getValue();
        }

        double r = Math.random() * totalWeight;
        double cumulativeWeight = 0;

        for (var e : eventChances.entrySet()) {
            cumulativeWeight += e.getValue();
            if (r < cumulativeWeight) {
                if (eventRegister.containsKey(e.getKey())) {
                    return eventRegister.get(e.getKey());
                } 
            }
        }

        return null;
    }

    private void removeCurrentEvent() {
        if (currentEvent == null)
            return;

        // Unregister the current event listener
        HandlerList.unregisterAll((Listener) currentEvent);

        if (currentEventRecord != null) {
            currentEventRecord.setEffective_end_time(System.currentTimeMillis());
            var warEvenRecordDbService = plugin.getDatabaseManager().getWarEventRecordDbService();
            warEvenRecordDbService.createOrUpdateAsync(currentEventRecord);
        } else {
            plugin.getLogger().warning("No event record found to update.");
        }

        currentEvent = null;
        currentEventStartTime = null;
        currentEventRecord = null;
    }

}
