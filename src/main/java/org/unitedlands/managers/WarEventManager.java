package org.unitedlands.managers;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.warevents.BaseWarEvent;
import org.unitedlands.models.WarEventRecord;
import org.unitedlands.util.MessageProvider;
import org.unitedlands.utils.DiscordService;
import org.unitedlands.utils.Logger;
import org.unitedlands.utils.Messenger;
import org.bukkit.event.HandlerList;

public class WarEventManager {

    private final UnitedWar plugin;
    private final MessageProvider messageProvider;

    private Set<String> eventRegister = new HashSet<>();
    private Map<String, Double> eventChances = new HashMap<>();
    private List<String> eventSchedule;
    private String skippedEventTime = null;

    private BaseWarEvent currentEvent = null;
    private WarEventRecord currentEventRecord = null;

    public WarEventManager(UnitedWar plugin, MessageProvider messageProvider) {
        this.plugin = plugin;
        this.messageProvider = messageProvider;
        buildEventRegister();
    }

    public void buildEventRegister() {

        eventRegister = new HashSet<>();
        eventChances = new HashMap<>();

        var eventsTable = plugin.getConfig().getConfigurationSection("war-events.events");
        for (var eventKey : eventsTable.getKeys(false)) {
            eventRegister.add(eventKey);
        }

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

    //#region Publc utility functions

    public Set<String> getEventRegister() {
        return eventRegister;
    }

    public void handleEvents() {
        handleCurrentEvent();
        if (currentEvent == null) {
            if (plugin.getWarManager().isAnyWarActive()) {
                handleEventCreation();
            }
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

    //#endregion

    //#region Event handling

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

    //#endregion

    //#region Event creation

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

            saveEventRecord();
            sendEventPickNotification();
        } else {
            Logger.logError("No event was picked. Please check the event register.");
        }
    }

    // Pick chances are normalized, ensuring that an event is always picked regardless of whether
    // the pick chances add up to 1
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
                if (eventRegister.contains(e.getKey())) {
                    return createBaseWarEventInstance(e.getKey());
                }
            }
        }

        return null;
    }

    public void forceEvent(Player sender, String eventName, Integer warmup) {
        if (!eventRegister.contains(eventName)) {
            Messenger.sendMessage(sender, messageProvider.get("messages.wa-warevents-unregistered"), null, messageProvider.get("messages.prefix"));
            return;
        }

        if (currentEvent != null)
            removeCurrentEvent();

        currentEvent = createBaseWarEventInstance(eventName);
        currentEvent.setScheduledStartTime(System.currentTimeMillis() + (warmup * 60000)); // warmup is in minutes
        currentEvent.setScheduledEndTime(currentEvent.getScheduledStartTime() + (currentEvent.getDuration() * 1000L));
        currentEvent.setActive(false);

        Bukkit.getPluginManager().registerEvents((Listener) currentEvent, plugin);

        saveEventRecord();

        sendEventPickNotification();
    }

    // Since war events might run custom logic inside of their constructor, we need to create a new
    // instance from the war event id using reflection at runtime whenever an event is picked,
    // loaded or forced
    private BaseWarEvent createBaseWarEventInstance(String eventId) {
        var className = plugin.getConfig().getString("war-events.events." + eventId + ".class");
        if (className == null)
            return null;

        try {
            Class<?> clazz = Class.forName("org.unitedlands.classes.warevents." + className);
            Object instance = clazz.getDeclaredConstructor().newInstance();

            BaseWarEvent warEvent = (BaseWarEvent) instance;
            warEvent.setDisplayname(plugin.getConfig().getString("war-events.events." + eventId + ".display-name"));
            warEvent.setDescription(plugin.getConfig().getString("war-events.events." + eventId + ".description"));
            warEvent.setDuration(plugin.getConfig().getLong("war-events.events." + eventId + ".duration"));
            warEvent.setInternalName(eventId);

            return warEvent;
        } catch (Exception ex) {
            Logger.logError("Couldn't create WarEvent class instance: " + ex.getMessage());
            return null;
        }
    }

    //#endregion

    //#region Database interaction

    public void loadEventRecord() {

        var warEvenRecordDbService = plugin.getDatabaseManager().getWarEventRecordDbService();
        warEvenRecordDbService.getIncompleteAsync().thenAccept(record -> {
            if (record != null) {
                currentEventRecord = record;
                currentEvent = createBaseWarEventInstance(record.getEvent_type());
                if (currentEvent != null) {
                    currentEvent.setScheduledStartTime(record.getScheduled_start_time());
                    currentEvent.setScheduledEndTime(record.getScheduled_end_time());
                    var currentTime = System.currentTimeMillis();
                    if (currentTime >= currentEvent.getScheduledStartTime()
                            && currentTime < currentEvent.getScheduledEndTime()) {
                        currentEvent.setActive(true);
                        Bukkit.getPluginManager().registerEvents((Listener) currentEvent, plugin);
                        Logger.log("Registered listeners for event: " + currentEvent.getDisplayname());
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
            Logger.logError("Failed to load war event record: " + ex.getMessage());
            return null;
        });
    }

    private void saveEventRecord() {
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

    //#endregion

    //#region Notification methods

    private void sendEventPickNotification() {
        Messenger.sendMessage(Bukkit.getServer(), messageProvider.getList("messages.event-info-scheduled"), currentEvent.getMessagePlaceholders());
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f);
        }

        if (plugin.getConfig().getBoolean("discord.enabled", false)) {
            var webhookUrl = plugin.getConfig().getString("discord.webhook_url");
            var pingrole = plugin.getConfig().getString("discord.ping-role-id");
            var embed = plugin.getConfig().getString("discord.war-event-schedule-embed");
            if (embed != null) {
                DiscordService.sendDiscordEmbed(webhookUrl, embed, pingrole, currentEvent.getMessagePlaceholders());
            }
        }
    }

    private void sendEventStartNotification() {
        Messenger.sendMessage(Bukkit.getServer(), messageProvider.getList("messages.event-info-active"), currentEvent.getMessagePlaceholders());
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.0f);
        }

        if (plugin.getConfig().getBoolean("discord.enabled", false)) {
            var webhookUrl = plugin.getConfig().getString("discord.webhook_url");
            var pingrole = plugin.getConfig().getString("discord.ping-role-id");
            var embed = plugin.getConfig().getString("discord.war-event-start-embed");
            if (embed != null) {
                DiscordService.sendDiscordEmbed(webhookUrl, embed, pingrole, currentEvent.getMessagePlaceholders());
            }
        }
    }

    private void sendEventEndNotification() {
        Messenger.sendMessage(Bukkit.getServer(), messageProvider.getList("messages.event-info-ended"), currentEvent.getMessagePlaceholders());
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.0f);
        }

        if (plugin.getConfig().getBoolean("discord.enabled", false)) {
            var webhookUrl = plugin.getConfig().getString("discord.webhook_url");
            var pingrole = plugin.getConfig().getString("discord.ping-role-id");
            var embed = plugin.getConfig().getString("discord.war-event-end-embed");
            if (embed != null) {
                DiscordService.sendDiscordEmbed(webhookUrl, embed, pingrole, currentEvent.getMessagePlaceholders());
            }
        }
    }

    //#endregion

    //#region Event removal

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

    //#endregion

}
