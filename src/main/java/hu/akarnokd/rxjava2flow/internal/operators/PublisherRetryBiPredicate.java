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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import hu.akarnokd.rxjava2flow.internal.subscriptions.SubscriptionArbiter;

import java.util.concurrent.Flow.*;

public final class PublisherRetryBiPredicate<T> implements Publisher<T> {
    final Publisher<? extends T> source;
    final BiPredicate<? super Integer, ? super Throwable> predicate;
    public PublisherRetryBiPredicate(
            Publisher<? extends T> source, 
            BiPredicate<? super Integer, ? super Throwable> predicate) {
        this.source = source;
        this.predicate = predicate;
    }
    
    @Override
    public void subscribe(Subscriber<? super T> s) {
        SubscriptionArbiter sa = new SubscriptionArbiter();
        s.onSubscribe(sa);
        
        RetryBiSubscriber<T> rs = new RetryBiSubscriber<>(s, predicate, sa, source);
        rs.subscribeNext();
    }
    
    static final class RetryBiSubscriber<T> extends AtomicInteger implements Subscriber<T> {
        /** */
        private static final long serialVersionUID = -7098360935104053232L;
        
        final Subscriber<? super T> actual;
        final SubscriptionArbiter sa;
        final Publisher<? extends T> source;
        final BiPredicate<? super Integer, ? super Throwable> predicate;
        int retries;
        public RetryBiSubscriber(Subscriber<? super T> actual, 
                BiPredicate<? super Integer, ? super Throwable> predicate, SubscriptionArbiter sa, Publisher<? extends T> source) {
            this.actual = actual;
            this.sa = sa;
            this.source = source;
            this.predicate = predicate;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            sa.setSubscription(s);
        }
        
        @Override
        public void onNext(T t) {
            actual.onNext(t);
            sa.produced(1L);
        }
        @Override
        public void onError(Throwable t) {
            boolean b;
            try {
                b = predicate.test(++retries, t);
            } catch (Throwable e) {
                e.addSuppressed(t);
                actual.onError(e);
                return;
            }
            if (!b) {
                actual.onError(t);
                return;
            }
            subscribeNext();
        }
        
        @Override
        public void onComplete() {
            actual.onComplete();
        }
        
        /**
         * Subscribes to the source again via trampolining.
         */
        void subscribeNext() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                for (;;) {
                    if (sa.isCancelled()) {
                        return;
                    }
                    source.subscribe(this);
                    
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }
    }
}
