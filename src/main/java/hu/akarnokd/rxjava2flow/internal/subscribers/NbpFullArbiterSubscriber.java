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

package hu.akarnokd.rxjava2flow.internal.subscribers;

import hu.akarnokd.rxjava2flow.NbpObservable.NbpSubscriber;
import hu.akarnokd.rxjava2flow.disposables.Disposable;
import hu.akarnokd.rxjava2flow.internal.disposables.NbpFullArbiter;
import hu.akarnokd.rxjava2flow.internal.subscriptions.SubscriptionHelper;

/**
 * Subscriber that communicates with a FullArbiter.
 *
 * @param <T> the value type
 */
public final class NbpFullArbiterSubscriber<T> implements NbpSubscriber<T> {
    final NbpFullArbiter<T> arbiter;

    Disposable s;

    public NbpFullArbiterSubscriber(NbpFullArbiter<T> arbiter) {
        this.arbiter = arbiter;
    }

    @Override
    public void onSubscribe(Disposable s) {
        if (SubscriptionHelper.validateDisposable(this.s, s)) {
            return;
        }
        this.s = s;
        arbiter.setSubscription(s);
    }

    @Override
    public void onNext(T t) {
        arbiter.onNext(t, s);
    }

    @Override
    public void onError(Throwable t) {
        arbiter.onError(t, s);
    }

    @Override
    public void onComplete() {
        arbiter.onComplete(s);
    }
}