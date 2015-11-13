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

import java.util.concurrent.*;

import hu.akarnokd.rxjava2flow.Scheduler;
import hu.akarnokd.rxjava2flow.internal.schedulers.*;
import hu.akarnokd.rxjava2flow.plugins.RxJavaPlugins;

public final class Schedulers {
    
    /*
     * TODO I started to like enums for singletons and non-instantiatable
     * utility classes, but since this is part of the public API,
     * that would act quite unorthodoxically.
     */
    private Schedulers() {
        throw new IllegalStateException("No instances");
    }

    static final Scheduler SINGLE;
    
    static final Scheduler COMPUTATION;
    
    static final Scheduler IO;
    
    static final Scheduler TRAMPOLINE;
    
    static final Scheduler NEW_THREAD;
    
    static {
        // TODO plugins and stuff
        SINGLE = RxJavaPlugins.initSingleScheduler(new SingleScheduler());
        
        COMPUTATION = RxJavaPlugins.initComputationScheduler(new ComputationScheduler());
        
        IO = RxJavaPlugins.initIOScheduler(new IOScheduler());
        
        TRAMPOLINE = TrampolineScheduler.instance();
        
        NEW_THREAD = RxJavaPlugins.initNewThreadScheduler(NewThreadScheduler.instance());
    }
    
    public static Scheduler computation() {
        return RxJavaPlugins.onComputationScheduler(COMPUTATION);
    }
    
    public static Scheduler io() {
        return RxJavaPlugins.onIOScheduler(IO);
    }
    
    public static TestScheduler test() {
        return new TestScheduler();
    }

    public static Scheduler trampoline() {
        return TRAMPOLINE;
    }

    public static Scheduler newThread() {
        return RxJavaPlugins.onNewThreadScheduler(NEW_THREAD);
    }
    
    /*
     * TODO This is a deliberately single threaded scheduler.
     * I can see a few uses for such scheduler:
     * - main event loop
     * - support Schedulers.from(Executor) and from(ExecutorService) with delayed scheduling.
     * - support benchmarks that pipeline data from the main thread to some other thread and avoid core-bashing of computation's round-robin nature. 
     */
    public static Scheduler single() {
        return RxJavaPlugins.onSingleScheduler(SINGLE);
    }
    
    // TODO I don't think immediate scheduler should be supported any further
    @Deprecated
    public static Scheduler immediate() {
        throw new UnsupportedOperationException();
    }
    
    public static Scheduler from(Executor executor) {
        return new ExecutorScheduler(executor);
    }
    
    /**
     * Shuts down all standard schedulers: computation, io, newThread, single and trampoline.
     * <p>The method is threadsafe and idempotent with respect to other calls to it until the
     * {@link #start()} method is called.
     * <p>Note that this may cut streams in half and they may end up hanging indefinitely.
     * Make sure you cancel all outstanding streams before you shut down the standard schedulers.
     * <p>Schedulers created from Executors via {@link #from(Executor)} are not affected.
     */
    public static void shutdown() {
        computation().shutdown();
        io().shutdown();
        newThread().shutdown();
        single().shutdown();
        trampoline().shutdown();
        SchedulerPoolHelper.shutdown();
    }
    
    /**
     * Starts up all standard schedulers: computation, io, newThread, single and trampoline.
     */
    public static void start() {
        SchedulerPoolHelper.start();
        computation().start();
        io().start();
        newThread().start();
        single().start();
        trampoline().start();
    }
}
