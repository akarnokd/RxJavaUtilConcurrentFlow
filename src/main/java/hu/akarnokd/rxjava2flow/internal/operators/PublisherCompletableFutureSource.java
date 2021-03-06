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

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.Flow.*;

import hu.akarnokd.rxjava2flow.internal.subscriptions.ScalarAsyncSubscription;

/**
 *
 */
public final class PublisherCompletableFutureSource<T> implements Publisher<T> {
    final CompletableFuture<? extends T> future;
    public PublisherCompletableFutureSource(CompletableFuture<? extends T> future) {
        this.future = future;
    }
    @Override
    public void subscribe(Subscriber<? super T> s) {
        ScalarAsyncSubscription<T> sub = new ScalarAsyncSubscription<>(s);
        s.onSubscribe(sub);
        
        future.whenComplete((v, e) -> {
            if (e != null) {
                s.onError(e);
            } else
            if (v == null) {
                s.onError(new NullPointerException());
            } else {
                sub.setValue(v);
            }
        });
    }
}
