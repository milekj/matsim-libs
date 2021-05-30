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

    public static final Logger logger = Logger.getRootLogger();
    private final Runnable processingRunnable;
    BlockingDeque<Double> stepsQueue;
    Map<Double, CountDownLatch> stepFinishedLatches;
    Map<Double, BlockingQueue<Event>> eventsQueues;
    private final int workersNumber;
    private Thread processingThread;
    private ParallelEventsManagerImpl delegate;
    private ForkJoinPool forkJoinPool;

    @Inject
    public MasterEventsManager(Config config) {
        MySimConfig mySimConfig = (MySimConfig) config.getModules().get("mySimConfig");
        workersNumber = mySimConfig.getWorkersNumber();

        int eventsThreadsNumber = config.parallelEventHandling().getNumberOfThreads() - 1;

        int poolThreadsNumber = config.global().getNumberOfThreads() - eventsThreadsNumber - 1;
        forkJoinPool = new ForkJoinPool(poolThreadsNumber);

        delegate = new ParallelEventsManagerImpl(eventsThreadsNumber);
        processingRunnable = () -> {
            logger.info("Events thread started");
            while (!Thread.currentThread().isInterrupted()) {
//                Logger.getRootLogger().info("Events thread loop");
                Double step = null;
                try {
//                    Logger.getRootLogger().info("Events thread Waiting for stepsQueue");
                    step = stepsQueue.takeFirst();

                    if (step % 1000 == 0)
                        logger.info("Processing events from step " + step);
//                    Logger.getRootLogger().info("Events thread Step taken from queue " + step);
                    CountDownLatch stepLatch = stepFinishedLatches.get(step);
                    stepLatch.await();
//                    Logger.getRootLogger().info("Events thread Step ready " + step);
                    BlockingQueue<Event> events = eventsQueues.get(step);
                    if (events == null) {
//                        Logger.getRootLogger().info("Events thread no events to process for step " + step);
                        continue;
                    }
                    LinkedList<Event>  currentStepEvents = new LinkedList<>();
                    events.drainTo(currentStepEvents);
                    delegate.processEvents(currentStepEvents);
//                    Logger.getRootLogger().info("Events thread in step processed " + step);
                } catch (InterruptedException e) {
                    logger.info("Events thread interrupted, step: " + step, e);
                    if (step != null)
                        stepsQueue.addFirst(step);
                    break;
                }
            }
            logger.info("Events thread finished processing");
        };
    }

    public void workerAfterSimStep(double step) {
        CountDownLatch stepLatch = stepFinishedLatches.get(step);
        if (stepLatch != null) {
            stepLatch.countDown();
//            logger.info("Counting down for: " + step);
        }
        else {
            stepFinishedLatches.put(step, new CountDownLatch(workersNumber - 1));
            stepsQueue.addLast(step);
//            logger.info("New latch for: " + step);
        }
    }

    public void processBatch(List<EventDto> eventsDtos) {
        if (eventsDtos.isEmpty())
            return;
        double time = eventsDtos.get(0).getTime();
        eventsQueues.computeIfAbsent(time, t -> new LinkedBlockingQueue<>());


        try {
            forkJoinPool.submit(() -> {
                List<Event> events = eventsDtos.parallelStream()
                        .map(EventsMapper::map)
                        .collect(Collectors.toList());
                eventsQueues.get(time).addAll(events);
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception while processing events batch", e);
        }

    }

    @Override
    public void processEvent(Event event) {
        logger.warn("Process event called on MasterEventsManager ");
        //parking cos handler...
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
        System.out.println("Trying to init processing");
        System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
        if (processingThread != null)
            return;
        System.out.println("Not already init");
        logger.info("Events processing started");
        delegate.initProcessing();
        stepsQueue = new LinkedBlockingDeque<>();
        stepFinishedLatches = new ConcurrentHashMap<>();
        eventsQueues = new ConcurrentHashMap<>();
        //todo
        processingThread = new Thread(processingRunnable);
        processingThread.start();
    }

    @Override
    public void afterSimStep(double time) {
        delegate.afterSimStep(time);
    }

    @Override
    public void finishProcessing() {
        System.out.println("finishing processing");
        if (processingThread == null) {
            System.out.println("already finished");
            return;
        }
        processingThread.interrupt();
        try {
            processingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        logger.info("Remaining steps: " + stepsQueue.size());
//        logger.info("First event: " + stepsQueue.peekFirst());
//        logger.info("Last event: " + stepsQueue.peekLast());
        while (!stepsQueue.isEmpty()) {
            try {
                Double step = stepsQueue.pollFirst();
                if (step % 1000 == 0)
                    logger.info("Processing events from step " + step);
                CountDownLatch stepLatch = stepFinishedLatches.get(step);
                stepLatch.await();
//                logger.info("Processing events from step " + step);
                BlockingQueue<Event> events = eventsQueues.get(step);
                if (events == null) {
                    continue;
                }
//                logger.info("After latch " + step);
                LinkedList<Event>  currentStepEvents = new LinkedList<>();
                events.drainTo(currentStepEvents);
                delegate.processEvents(currentStepEvents);
            } catch (InterruptedException e) {
                logger.error("interrupted - should not happen", e);
            }
        }

//        Object nonNull = eventsQueues.entrySet()
//                .stream()
//                .filter(e -> e.getValue() != null)
////                .map(Map.Entry::getKey)
//                .collect(Collectors.toList());

//        System.out.println("Remaining events = " + nonNull);

        processingThread = null;
        delegate.finishProcessing();
    }
}
