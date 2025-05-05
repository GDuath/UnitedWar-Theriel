package org.unitedlands.managers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.warevents.BaseWarEvent;
import org.unitedlands.classes.warevents.SampleEvent;
import org.unitedlands.models.WarEventRecord;
import org.unitedlands.util.Formatter;
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

    private void buildEventRegister() {
        eventRegister.put("SAMPLE1", new SampleEvent("SAMPLE1", "Sample event 1", "Lorem ipsum dolor sit amet.", 120L));
        eventRegister.put("SAMPLE2", new SampleEvent("SAMPLE2", "Sample event 2", "Lorem ipsum dolor sit amet.", 120L));
        eventRegister.put("SAMPLE3", new SampleEvent("SAMPLE3", "Sample event 3", "Lorem ipsum dolor sit amet.", 120L));
        eventRegister.put("SAMPLE4", new SampleEvent("SAMPLE4", "Sample event 4", "Lorem ipsum dolor sit amet.", 120L));

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

    public void loadEventRecord() {
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
                    plugin.getLogger().info("Loaded ongoing event: " + currentEvent.getDisplayname());
                } else {
                    plugin.getLogger().severe(
                            "No event found for the loaded ongoing record. Event type: " + record.getEvent_type());
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
        plugin.getLogger().info("Event reset.");
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
                sendEventEndNotification();
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
                    plugin.getLogger().info("Event creation skipped for " + timeStr + " due to random chance.");
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

            currentEventRecord = new WarEventRecord() {
                {
                    setTimestamp(System.currentTimeMillis());
                    setScheduled_start_time(currentEvent.getScheduledStartTime());
                    setScheduled_end_time(currentEvent.getScheduledEndTime());
                    setEvent_type(currentEvent.getInternalName());
                }
            };
            plugin.getDatabaseManager().getWarEventRecordDbService().createOrUpdateAsync(currentEventRecord);

            sendEventPickNotification();
        } else {
            plugin.getLogger().warning("No event was picked. Please check the event register.");
        }
    }

    private void sendEventPickNotification() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("event-name", currentEvent.getDisplayname());
        placeholders.put("event-description", currentEvent.getDescription());
        placeholders.put("event-duration", String.valueOf(currentEvent.getDuration()));
        placeholders.put("event-relative-start", Formatter.formatDuration(currentEvent.getScheduledStartTime() - System.currentTimeMillis()));
        placeholders.put("event-duration", Formatter.formatDuration(currentEvent.getScheduledEndTime() - currentEvent.getScheduledStartTime()));
        Messenger.broadcastMessageListTemplate("event-info-scheduled", placeholders, false);
    }

    private void sendEventStartNotification() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("event-name", currentEvent.getDisplayname());
        placeholders.put("event-description", currentEvent.getDescription());
        placeholders.put("event-remaining-duration", Formatter.formatDuration(currentEvent.getScheduledEndTime() - System.currentTimeMillis()));
        Messenger.broadcastMessageListTemplate("event-info-active", placeholders, false);
    }

    private void sendEventEndNotification() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("event-name", currentEvent.getDisplayname());
        placeholders.put("event-description", currentEvent.getDescription());
        Messenger.broadcastMessageListTemplate("event-info-ended", placeholders, false);
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
