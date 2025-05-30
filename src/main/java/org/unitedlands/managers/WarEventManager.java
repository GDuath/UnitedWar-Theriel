package org.unitedlands.managers;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.warevents.BaseWarEvent;
import org.unitedlands.classes.warevents.SampleEvent;
import org.unitedlands.classes.warevents.WarLivesNoDeathWarEvent;
import org.unitedlands.classes.warevents.WarLivesSuddenDeathWarEvent;
import org.unitedlands.models.WarEventRecord;

import org.unitedlands.util.Logger;
import org.unitedlands.util.Messenger;
import org.bukkit.event.HandlerList;

public class WarEventManager {

    private final UnitedWar plugin;

    private Map<String, BaseWarEvent> eventRegister = new HashMap<>();
    private Map<String, Double> eventChances = new HashMap<>();
    private List<String> eventSchedule;
    private String skippedEventTime = null;

    private BaseWarEvent currentEvent = null;
    private WarEventRecord currentEventRecord = null;

    public WarEventManager(UnitedWar plugin) {
        this.plugin = plugin;
        buildEventRegister();
    }

    public void buildEventRegister() {

        eventRegister = new HashMap<>();
        eventChances = new HashMap<>();

        eventRegister.put("SAMPLE", new SampleEvent("SAMPLE", "Defenders’ Fury", "All defender PvP kills give double points.", 7200L));
        eventRegister.put("SUDDENDEATH", new WarLivesSuddenDeathWarEvent("SUDDENDEATH", "Death is Working Overtime", "Players instantly lose all remaining war lives when they die.", 7200L));
        eventRegister.put("NODEATH", new WarLivesNoDeathWarEvent("NODEATH", "Death is on Vacation", "Players don't lose any war lives when dying.", 7200L));

        var pickTable = plugin.getConfig().getConfigurationSection("war-events.pick-table");
        for (String key : pickTable.getKeys(false)) {
            eventChances.put(key, pickTable.getDouble(key, 0.5));
        }

        eventSchedule = plugin.getConfig().getStringList("war-events.event-schedule");
        if (eventSchedule == null || eventSchedule.isEmpty()) {
            plugin.getLogger().warning("No event schedule found in the config. No events will be scheduled.");
            eventSchedule = List.of();
        }
    }

    public Map<String, BaseWarEvent> getEventRegister() {
        return eventRegister;
    }

    public void loadEventRecord() {
        Logger.log("War event records...");
        var warEvenRecordDbService = plugin.getDatabaseManager().getWarEventRecordDbService();
        warEvenRecordDbService.getIncompleteAsync().thenAccept(record -> {
            if (record != null) {
                currentEventRecord = record;
                currentEvent = eventRegister.get(record.getEvent_type());
                if (currentEvent != null) {
                    currentEvent.setScheduledStartTime(record.getScheduled_start_time());
                    currentEvent.setScheduledEndTime(record.getScheduled_end_time());
                    var currentTime = System.currentTimeMillis();
                    if (currentTime >= currentEvent.getScheduledStartTime()
                            && currentTime < currentEvent.getScheduledEndTime()) {
                        currentEvent.setActive(true);
                    } else {
                        removeCurrentEvent();
                    }
                    Logger.log("Loaded ongoing event: " + currentEvent.getDisplayname());
                } else {
                    Logger.logError(
                            "No event found for the loaded ongoing record. Event type: " + record.getEvent_type());
                }
            }
        }).exceptionally(ex -> {
            Logger.logError("Failed to load war event records: " + ex.getMessage());
            return null;
        });;
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

    public BaseWarEvent getCurrentEvent() {
        return currentEvent;
    }

    public void resetEvent() {
        if (currentEvent != null) {
            removeCurrentEvent();
        }
    }

    private void handleCurrentEvent() {
        if (currentEvent == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (!currentEvent.isActive()) {
            if (currentTime >= currentEvent.getScheduledStartTime()) {
                currentEvent.setActive(true);
                sendEventStartNotification();
            }
        } else {
            if (currentTime >= currentEvent.getScheduledEndTime()) {
                removeCurrentEvent();
            }
        }
    }

    private void handleEventCreation() {
        LocalTime now = LocalTime.now();
        for (String timeStr : eventSchedule) {
            LocalTime eventStart = LocalTime.parse(timeStr);

            if (timeStr.equals(skippedEventTime)) {
                continue;
            }

            // Check if we are within 15 seconds of an event start time. This accounts for
            // server lag.
            if (isNowWithin15Seconds(now, eventStart)) {
                double eventChance = plugin.getConfig().getDouble("war-events.random-event-chance", 0.5f);
                if (Math.random() < eventChance) {
                    pickRandomEvent();
                    skippedEventTime = null;
                } else {
                    Logger.log("Event creation skipped for " + timeStr + " due to random chance.");
                    skippedEventTime = timeStr;
                }
            }
        }
    }

    private boolean isNowWithin15Seconds(LocalTime now, LocalTime eventTime) {
        return Math.abs(Duration.between(now, eventTime).getSeconds()) <= 15;
    }

    private void pickRandomEvent() {
        var newEvent = doWeightedRandomSelection();
        if (newEvent != null) {
            var eventWarmupTime = plugin.getConfig().getInt("war-events.event-warmup-time", 0);
            currentEvent = newEvent;
            currentEvent.setScheduledStartTime(System.currentTimeMillis() + (eventWarmupTime * 1000L));
            currentEvent
                    .setScheduledEndTime(currentEvent.getScheduledStartTime() + (currentEvent.getDuration() * 1000L));
            currentEvent.setActive(false);

            Bukkit.getPluginManager().registerEvents((Listener) currentEvent, plugin);

            saveCurrentEventToDatabase();

            sendEventPickNotification();
        } else {
            plugin.getLogger().warning("No event was picked. Please check the event register.");
        }
    }

    public void forceEvent(Player sender, String eventName, Integer warmup)
    {
        if (!eventRegister.containsKey(eventName)) {
            Messenger.sendMessage(sender, "This internal event name is not registered", true);
            return;
        }

        if (currentEvent != null)
            removeCurrentEvent();

        currentEvent = eventRegister.get(eventName);
        currentEvent.setScheduledStartTime(System.currentTimeMillis() + (warmup * 60000)); // warmup is in minutes
        currentEvent.setScheduledEndTime(currentEvent.getScheduledStartTime() + (currentEvent.getDuration() * 1000L));
        currentEvent.setActive(false);

        Bukkit.getPluginManager().registerEvents((Listener) currentEvent, plugin);

        saveCurrentEventToDatabase();

        sendEventPickNotification();
    }

    private void saveCurrentEventToDatabase() {
        currentEventRecord = new WarEventRecord() {
            {
                setTimestamp(System.currentTimeMillis());
                setScheduled_start_time(currentEvent.getScheduledStartTime());
                setScheduled_end_time(currentEvent.getScheduledEndTime());
                setEvent_type(currentEvent.getInternalName());
            }
        };
        plugin.getDatabaseManager().getWarEventRecordDbService().createOrUpdateAsync(currentEventRecord);
    }

    private void sendEventPickNotification() {
        Messenger.broadcastMessageListTemplate("event-info-scheduled", currentEvent.getMessagePlaceholders(), false);
    }

    private void sendEventStartNotification() {
        Messenger.broadcastMessageListTemplate("event-info-active", currentEvent.getMessagePlaceholders(), false);
    }

    private void sendEventEndNotification() {
        Messenger.broadcastMessageListTemplate("event-info-ended", currentEvent.getMessagePlaceholders(), false);
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

        sendEventEndNotification();

        // Unregister the current event listener
        HandlerList.unregisterAll((Listener) currentEvent);

        currentEvent.setActive(false);
        currentEvent.setScheduledStartTime(null);
        currentEvent.setScheduledEndTime(null);

        if (currentEventRecord != null) {
            currentEventRecord.setEffective_end_time(System.currentTimeMillis());
            var warEvenRecordDbService = plugin.getDatabaseManager().getWarEventRecordDbService();
            warEvenRecordDbService.createOrUpdateAsync(currentEventRecord);
        } else {
            plugin.getLogger().warning("No event record found to update.");
        }

        currentEvent = null;
        currentEventRecord = null;
        skippedEventTime = null;
    }

}
