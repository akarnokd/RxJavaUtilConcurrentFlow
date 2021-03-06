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

import java.util.function.Supplier;

import hu.akarnokd.rxjava2flow.internal.subscriptions.EmptySubscription;

import java.util.concurrent.Flow.*;

/**
 *
 */
public final class PublisherDefer<T> implements Publisher<T> {
    final Supplier<? extends Publisher<? extends T>> supplier;
    public PublisherDefer(Supplier<? extends Publisher<? extends T>> supplier) {
        this.supplier = supplier;
    }
    @Override
    public void subscribe(Subscriber<? super T> s) {
        Publisher<? extends T> pub;
        try {
            pub = supplier.get();
        } catch (Throwable t) {
            EmptySubscription.error(t, s);
            return;
        }
        
        if (pub == null) {
            EmptySubscription.error(new NullPointerException("null publisher supplied"), s);
            return;
        }
        pub.subscribe(s);
    }
}
