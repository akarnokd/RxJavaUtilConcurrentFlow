/**
 * Copyright 2015 David Karnok and Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package hu.akarnokd.rxjava2flow.schedulers;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import hu.akarnokd.rxjava2flow.Scheduler;
import hu.akarnokd.rxjava2flow.Scheduler.Worker;
import hu.akarnokd.rxjava2flow.disposables.Disposable;
import hu.akarnokd.rxjava2flow.internal.schedulers.RxThreadFactory;

public class ExecutorSchedulerTest extends AbstractSchedulerConcurrencyTests {

    final static Executor executor = Executors.newFixedThreadPool(2, new RxThreadFactory("TestCustomPool-"));
    
    @Override
    protected Scheduler getScheduler() {
        return Schedulers.from(executor);
    }

    @Test
    @Ignore("Unhandled errors are no longer thrown")
    public final void testUnhandledErrorIsDeliveredToThreadHandler() throws InterruptedException {
        SchedulerTests.testUnhandledErrorIsDeliveredToThreadHandler(getScheduler());
    }

    @Test
    public final void testHandledErrorIsNotDeliveredToThreadHandler() throws InterruptedException {
        SchedulerTests.testHandledErrorIsNotDeliveredToThreadHandler(getScheduler());
    }
    
    
    
    @Test(timeout = 90000)
    public void testCancelledTaskRetention() throws InterruptedException {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Scheduler s = Schedulers.from(exec);
        try {
            SchedulerRetentionTest.testCancellationRetention(s, false);
        } finally {
            exec.shutdownNow();
        }
    }
    
    /** A simple executor which queues tasks and executes them one-by-one if executeOne() is called. */
    static final class TestExecutor implements Executor {
        final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
        @Override
        public void execute(Runnable command) {
            queue.offer(command);
        }
        public void executeOne() {
            Runnable r = queue.poll();
            if (r != null) {
                r.run();
            }
        }
        public void executeAll() {
            Runnable r;
            while ((r = queue.poll()) != null) {
                r.run();
            }
        }
    }
    
    @Test
    public void testCancelledTasksDontRun() {
        final AtomicInteger calls = new AtomicInteger();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        };
        TestExecutor exec = new TestExecutor();
        Scheduler custom = Schedulers.from(exec);
        Worker w = custom.createWorker();
        try {
            Disposable s1 = w.schedule(task);
            Disposable s2 = w.schedule(task);
            Disposable s3 = w.schedule(task);
            
            s1.dispose();
            s2.dispose();
            s3.dispose();
            
            exec.executeAll();
            
            assertEquals(0, calls.get());
        } finally {
            w.dispose();
        }
    }
    @Test
    public void testCancelledWorkerDoesntRunTasks() {
        final AtomicInteger calls = new AtomicInteger();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        };
        TestExecutor exec = new TestExecutor();
        Scheduler custom = Schedulers.from(exec);
        Worker w = custom.createWorker();
        try {
            w.schedule(task);
            w.schedule(task);
            w.schedule(task);
        } finally {
            w.dispose();
        }
        exec.executeAll();
        assertEquals(0, calls.get());
    }
    
    // FIXME the internal structure changed and these can't be tested
//    
//    @Test
//    public void testNoTimedTaskAfterScheduleRetention() throws InterruptedException {
//        Executor e = new Executor() {
//            @Override
//            public void execute(Runnable command) {
//                command.run();
//            }
//        };
//        ExecutorWorker w = (ExecutorWorker)Schedulers.from(e).createWorker();
//        
//        w.schedule(() -> { }, 50, TimeUnit.MILLISECONDS);
//        
//        assertTrue(w.tasks.hasSubscriptions());
//        
//        Thread.sleep(150);
//        
//        assertFalse(w.tasks.hasSubscriptions());
//    }
//    
//    @Test
//    public void testNoTimedTaskPartRetention() {
//        Executor e = new Executor() {
//            @Override
//            public void execute(Runnable command) {
//                
//            }
//        };
//        ExecutorWorker w = (ExecutorWorker)Schedulers.from(e).createWorker();
//        
//        Disposable s = w.schedule(() -> { }, 1, TimeUnit.DAYS);
//        
//        assertTrue(w.tasks.hasSubscriptions());
//        
//        s.dispose();
//        
//        assertFalse(w.tasks.hasSubscriptions());
//    }
//    
//    @Test
//    public void testNoPeriodicTimedTaskPartRetention() throws InterruptedException {
//        Executor e = new Executor() {
//            @Override
//            public void execute(Runnable command) {
//                command.run();
//            }
//        };
//        ExecutorWorker w = (ExecutorWorker)Schedulers.from(e).createWorker();
//        
//        final CountDownLatch cdl = new CountDownLatch(1);
//        final Runnable action = new Runnable() {
//            @Override
//            public void run() {
//                cdl.countDown();
//            }
//        };
//        
//        Disposable s = w.schedulePeriodically(action, 0, 1, TimeUnit.DAYS);
//        
//        assertTrue(w.tasks.hasSubscriptions());
//        
//        cdl.await();
//        
//        s.dispose();
//        
//        assertFalse(w.tasks.hasSubscriptions());
//    }
}