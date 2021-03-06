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

package hu.akarnokd.rxjava2flow.internal.operators;

import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import hu.akarnokd.rxjava2flow.Scheduler;
import hu.akarnokd.rxjava2flow.Observable.Operator;
import hu.akarnokd.rxjava2flow.internal.queue.SpscLinkedArrayQueue;
import hu.akarnokd.rxjava2flow.internal.subscriptions.SubscriptionHelper;
import hu.akarnokd.rxjava2flow.internal.util.BackpressureHelper;

import java.util.concurrent.Flow.*;

public final class OperatorTakeLastTimed<T> implements Operator<T, T> {
    final long count;
    final long time;
    final TimeUnit unit;
    final Scheduler scheduler;
    final int bufferSize;
    final boolean delayError;

    public OperatorTakeLastTimed(long count, long time, TimeUnit unit, Scheduler scheduler, int bufferSize, boolean delayError) {
        this.count = count;
        this.time = time;
        this.unit = unit;
        this.scheduler = scheduler;
        this.bufferSize = bufferSize;
        this.delayError = delayError;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        return new TakeLastTimedSubscriber<>(t, count, time, unit, scheduler, bufferSize, delayError);
    }
    
    static final class TakeLastTimedSubscriber<T> extends AtomicInteger implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -5677354903406201275L;
        final Subscriber<? super T> actual;
        final long count;
        final long time;
        final TimeUnit unit;
        final Scheduler scheduler;
        final Queue<Object> queue;
        final boolean delayError;
        
        Subscription s;
        
        volatile long requested;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<TakeLastTimedSubscriber> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(TakeLastTimedSubscriber.class, "requested");
        
        volatile boolean cancelled;
        
        volatile boolean done;
        Throwable error;

        public TakeLastTimedSubscriber(Subscriber<? super T> actual, long count, long time, TimeUnit unit, Scheduler scheduler, int bufferSize, boolean delayError) {
            this.actual = actual;
            this.count = count;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
            this.queue = new SpscLinkedArrayQueue<>(bufferSize);
            this.delayError = delayError;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
            s.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(T t) {
            final Queue<Object> q = queue;

            long now = scheduler.now(unit);

            q.offer(now);
            q.offer(t);
            
            trim(now, q);
        }
        
        @Override
        public void onError(Throwable t) {
            if (delayError) {
                trim(scheduler.now(unit), queue);
            }
            error = t;
            done = true;
            drain();
        }
        
        @Override
        public void onComplete() {
            trim(scheduler.now(unit), queue);
            done = true;
            drain();
        }
        
        void trim(long now, Queue<Object> q) {
            long time = this.time;
            long c = count;
            boolean unbounded = c == Long.MAX_VALUE;

            while (!q.isEmpty()) {
                long ts = (Long)q.peek();
                if (ts < now - time || (!unbounded && (q.size() >> 1) > c)) {
                    q.poll();
                    q.poll();
                } else {
                    break;
                }
            }
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            BackpressureHelper.add(REQUESTED, this, n);
            drain();
        }
        
        @Override
        public void cancel() {
            if (cancelled) {
                cancelled = true;
                
                if (getAndIncrement() == 0) {
                    queue.clear();
                    s.cancel();
                }
            }
        }
        
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            
            int missed = 1;
            
            final Subscriber<? super T> a = actual;
            final Queue<Object> q = queue;
            final boolean delayError = this.delayError;
            
            for (;;) {
                
                if (done) {
                    boolean empty = q.isEmpty();
                    
                    if (checkTerminated(empty, a, delayError)) {
                        return;
                    }
                    
                    long r = requested;
                    boolean unbounded = r == Long.MAX_VALUE;
                    long e = 0L;
                    
                    for (;;) {
                        Object ts = q.peek(); // the timestamp long
                        empty = ts == null;
                        
                        if (checkTerminated(empty, a, delayError)) {
                            return;
                        }
                        
                        if (empty || r == 0L) {
                            break;
                        }
                        
                        q.poll();
                        @SuppressWarnings("unchecked")
                        T o = (T)q.poll();
                        if (o == null) {
                            s.cancel();
                            a.onError(new IllegalStateException("Queue empty?!"));
                            return;
                        }
                        
                        a.onNext(o);
                        
                        r--;
                        e--;
                    }
                    
                    if (e != 0L) {
                        if (!unbounded) {
                            REQUESTED.addAndGet(this, e);
                        }
                    }
                }
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        boolean checkTerminated(boolean empty, Subscriber<? super T> a, boolean delayError) {
            if (cancelled) {
                queue.clear();
                s.cancel();
                return true;
            }
            if (delayError) {
                if (empty) {
                    Throwable e = error;
                    if (e != null) {
                        a.onError(e);
                    } else {
                        a.onComplete();
                    }
                    return true;
                }
            } else {
                Throwable e = error;
                if (e != null) {
                    queue.clear();
                    a.onError(e);
                    return true;
                } else
                if (empty) {
                    a.onComplete();
                    return true;
                }
            }
            return false;
        }
    }
}
