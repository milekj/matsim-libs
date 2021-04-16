package org.mjanowski.master;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ParallelEventHandlingConfigGroup;
import org.matsim.core.events.ParallelEventsManagerImpl;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.qsim.qnetsimengine.EventDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.EventsMapper;
import org.mjanowski.MySimConfig;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MasterEventsManager implements EventsManager {

    BlockingQueue<Double> stepsQueue = new LinkedBlockingQueue<>();
    Map<Double, CountDownLatch> stepFinishedLatches = new ConcurrentHashMap<>();
    Map<Double, Queue<Event>> eventsQueues = new HashMap<>();
    private final int workersNumber;
    private Thread processingThread;

    private EventsManager delegate;

    @Inject
    public MasterEventsManager(Config config) {
        MySimConfig mySimConfig = (MySimConfig) config.getModules().get("mySimConfig");
        workersNumber = mySimConfig.getWorkersNumber();
        ParallelEventHandlingConfigGroup eventHandlingConfig = config.parallelEventHandling();
        int threadsNumber = eventHandlingConfig != null && eventHandlingConfig.getNumberOfThreads() != null
                ? eventHandlingConfig.getNumberOfThreads() : 2;

        delegate = new ParallelEventsManagerImpl(threadsNumber);

        processingThread = new Thread(() -> {
            Logger.getRootLogger().info("Events thread started");
            while (!Thread.currentThread().isInterrupted()) {
//                Logger.getRootLogger().info("Events thread loop");
                try {
//                    Logger.getRootLogger().info("Events thread Waiting for stepsQueue");
                    Double step = stepsQueue.take();

                    if (step % 1000 == 0)
                        Logger.getRootLogger().info("Processing events from step " + step);
//                    Logger.getRootLogger().info("Events thread Step taken from queue " + step);
                    CountDownLatch stepLatch = stepFinishedLatches.get(step);
                    stepLatch.await();
//                    Logger.getRootLogger().info("Events thread Step ready " + step);
                    Queue<Event> events = eventsQueues.get(step);
                    if (events == null) {
//                        Logger.getRootLogger().info("Events thread no events to process for step " + step);
                        continue;
                    }
                    events.forEach(e -> delegate.processEvent(e));
//                    Logger.getRootLogger().info("Events thread in step processed " + step);
                } catch (InterruptedException e) {
//                    Logger.getRootLogger().info("Events thread interrupted");
                    break;
                }
            }
            Logger.getRootLogger().info("Events thread finished processing");
        });
    }

    public void workerAfterSimStep(double step) {
//        Logger.getRootLogger().info("Worker finished step " + step);
        CountDownLatch stepLatch = stepFinishedLatches.get(step);
        if (stepLatch != null)
            stepLatch.countDown();
        else {
            stepFinishedLatches.put(step, new CountDownLatch(workersNumber - 1));
//            Logger.getLogger("Added to stepsQueue " + step);
            stepsQueue.add(step);
        }
    }

    public void processBatch(List<EventDto> eventsDtos) {
        if (eventsDtos.isEmpty())
            return;
        double time = eventsDtos.get(0).getTime();
        eventsQueues.computeIfAbsent(time, t -> new LinkedList<>());
        List<Event> events = eventsDtos.stream()
                .map(EventsMapper::map)
                .collect(Collectors.toList());
        eventsQueues.get(time).addAll(events);
    }

    @Override
    public void processEvent(Event event) {
        Logger.getRootLogger().error("Process event called on MasterEventsManager");
        //todo?
        //raczej nigdy to nie powinno być wołane
    }

    @Override
    public void addHandler(EventHandler handler) {
        delegate.addHandler(handler);
    }

    @Override
    public void removeHandler(EventHandler handler) {
        delegate.removeHandler(handler);
    }

    @Override
    public void resetHandlers(int iteration) {
        delegate.resetHandlers(iteration);
    }

    @Override
    public void initProcessing() {
        Logger.getRootLogger().info("Events processing started");
        processingThread.start();
        delegate.initProcessing();
    }

    @Override
    public void afterSimStep(double time) {
        delegate.afterSimStep(time);
    }

    @Override
    public void finishProcessing() {
        processingThread.interrupt();
        delegate.finishProcessing();
    }
}
