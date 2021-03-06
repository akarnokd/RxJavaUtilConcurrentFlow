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

package hu.akarnokd.rxjava2flow.internal.operators.completable;

import java.util.concurrent.atomic.*;

import hu.akarnokd.rxjava2flow.*;
import hu.akarnokd.rxjava2flow.Completable.*;
import hu.akarnokd.rxjava2flow.disposables.Disposable;
import hu.akarnokd.rxjava2flow.exceptions.MissingBackpressureException;
import hu.akarnokd.rxjava2flow.internal.disposables.SerialResource;
import hu.akarnokd.rxjava2flow.internal.queue.SpscArrayQueue;
import hu.akarnokd.rxjava2flow.internal.subscriptions.SubscriptionHelper;
import hu.akarnokd.rxjava2flow.plugins.RxJavaPlugins;

import java.util.concurrent.Flow.*;

public final class CompletableOnSubscribeConcat implements CompletableOnSubscribe {
    final Observable<? extends Completable> sources;
    final int prefetch;
    
    public CompletableOnSubscribeConcat(Observable<? extends Completable> sources, int prefetch) {
        this.sources = sources;
        this.prefetch = prefetch;
    }
    
    @Override
    public void accept(CompletableSubscriber s) {
        CompletableConcatSubscriber parent = new CompletableConcatSubscriber(s, prefetch);
        sources.subscribe(parent);
    }
    
    static final class CompletableConcatSubscriber
    extends AtomicInteger
    implements Subscriber<Completable>, Disposable {
        /** */
        private static final long serialVersionUID = 7412667182931235013L;
        final CompletableSubscriber actual;
        final int prefetch;
        final SerialResource<Disposable> sr;
        
        final SpscArrayQueue<Completable> queue;
        
        Subscription s;
        
        volatile boolean done;

        volatile int once;
        static final AtomicIntegerFieldUpdater<CompletableConcatSubscriber> ONCE =
                AtomicIntegerFieldUpdater.newUpdater(CompletableConcatSubscriber.class, "once");
        
        final ConcatInnerSubscriber inner;
        
        public CompletableConcatSubscriber(CompletableSubscriber actual, int prefetch) {
            this.actual = actual;
            this.prefetch = prefetch;
            this.queue = new SpscArrayQueue<>(prefetch);
            this.sr = new SerialResource<>(Disposable::dispose);
            this.inner = new ConcatInnerSubscriber();
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
            s.request(prefetch);
        }
        
        @Override
        public void onNext(Completable t) {
            if (!queue.offer(t)) {
                onError(new MissingBackpressureException());
                return;
            }
            if (getAndIncrement() == 0) {
                next();
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (ONCE.compareAndSet(this, 0, 1)) {
                actual.onError(t);
                return;
            }
            RxJavaPlugins.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            if (getAndIncrement() == 0) {
                next();
            }
        }
        
        void innerError(Throwable e) {
            s.cancel();
            onError(e);
        }
        
        void innerComplete() {
            if (decrementAndGet() != 0) {
                next();
            }
            if (!done) {
                s.request(1);
            }
        }
        
        @Override
        public void dispose() {
            s.cancel();
            sr.dispose();
        }
        
        void next() {
            boolean d = done;
            Completable c = queue.poll();
            if (c == null) {
                if (d) {
                    if (ONCE.compareAndSet(this, 0, 1)) {
                        actual.onComplete();
                    }
                    return;
                }
                RxJavaPlugins.onError(new IllegalStateException("Queue is empty?!"));
                return;
            }
            
            c.subscribe(inner);
        }
        
        final class ConcatInnerSubscriber implements CompletableSubscriber {
            @Override
            public void onSubscribe(Disposable d) {
                sr.set(d);
            }
            
            @Override
            public void onError(Throwable e) {
                innerError(e);
            }
            
            @Override
            public void onComplete() {
                innerComplete();
            }
        }
    }
}
