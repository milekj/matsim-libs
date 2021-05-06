package org.mjanowski.worker;

import com.google.common.util.concurrent.ForwardingBlockingDeque;
import com.google.inject.Inject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.qsim.WorkerDelegate;
import org.matsim.core.mobsim.qsim.qnetsimengine.EventDto;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WorkerEventsManager implements EventsManager {

    public static final int BATCH_SIZE = 1000;
    private final WorkerDelegate workerDelegate;
    private final BlockingDeque<Event> eventsQueue;
    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
    private final Runnable sendingThread;

    @Inject
    public WorkerEventsManager(WorkerDelegate workerDelegate) {
        this.workerDelegate = workerDelegate;
        eventsQueue = new LinkedBlockingDeque<>();
        sendingThread = () -> {
            synchronized (eventsQueue) {
                while (eventsQueue.size() >= BATCH_SIZE) {
                    List<Event> events = new LinkedList<>();
                    eventsQueue.drainTo(events, BATCH_SIZE);
//                    Logger.getRootLogger().info("Sending events from scheduled, first time " + events.get(0).getTime() + " last time " + events.get(events.size() - 1).getTime());
                    sendEvents(events);
                }
            }
        };
    }

    @Override
    public void processEvent(Event event) {
//        if (event instanceof PersonStuckEvent)
//        Logger.getRootLogger().info("Person stuck \n" + ExceptionUtils.getStackTrace(new Throwable()));
//        Logger.getRootLogger().info("Processing event time " + event.getTime());
        eventsQueue.add(event);
//        Logger.getRootLogger().info("Stopped processing event time " + event.getTime());
    }

    @Override
    public void addHandler(EventHandler handler) {

    }

    @Override
    public void removeHandler(EventHandler handler) {

    }

    @Override
    public void resetHandlers(int iteration) {

    }

    @Override
    public void initProcessing() {
        scheduleProcessing();
    }

    @Override
    public void afterSimStep(double time) {
//        Logger.getRootLogger().info("Starting afterSimStep time " + time);
        synchronized (eventsQueue) {
            sendAllEvents();
        }
//        Logger.getRootLogger().info("Finished afterSimStep time " + time);
        workerDelegate.sendAfterSimStep(time);
    }

    @Override
    public void finishProcessing() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendAllEvents();
        executorService = new ScheduledThreadPoolExecutor(1);
    }

    private void scheduleProcessing() {
        executorService.scheduleAtFixedRate(sendingThread, 0, 1, TimeUnit.SECONDS);
    }

    private void sendAllEvents() {

        while (!eventsQueue.isEmpty()) {
            List<Event> events = new LinkedList<>();
            eventsQueue.drainTo(events, BATCH_SIZE);
//        Logger.getRootLogger().info("Sending events from sendAllEvents, first time " + events.get(0).getTime() + " last time " + events.get(events.size() - 1).getTime());
            if (!events.isEmpty())
                sendEvents(events);
//        Logger.getRootLogger().info("Queue after sending all events, first time " + eventsQueue);
        }
    }

    private void sendEvents(List<Event> events) {
        List<EventDto> batch = events.stream()
                .map(e -> new EventDto(e.getTime(), e.getEventType(), e.getAttributes()))
                .collect(Collectors.toList());
        workerDelegate.sendEvents(batch);
    }
}
