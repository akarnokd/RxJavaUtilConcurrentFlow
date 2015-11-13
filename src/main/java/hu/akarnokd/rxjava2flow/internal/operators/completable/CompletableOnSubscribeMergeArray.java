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

import hu.akarnokd.rxjava2flow.Completable;
import hu.akarnokd.rxjava2flow.Completable.*;
import hu.akarnokd.rxjava2flow.disposables.*;
import hu.akarnokd.rxjava2flow.plugins.RxJavaPlugins;

public final class CompletableOnSubscribeMergeArray implements CompletableOnSubscribe {
    final Completable[] sources;
    
    public CompletableOnSubscribeMergeArray(Completable[] sources) {
        this.sources = sources;
    }
    
    @Override
    public void accept(CompletableSubscriber s) {
        CompositeDisposable set = new CompositeDisposable();
        AtomicInteger wip = new AtomicInteger(sources.length + 1);
        AtomicBoolean once = new AtomicBoolean();
        
        s.onSubscribe(set);
        
        for (Completable c : sources) {
            if (set.isDisposed()) {
                return;
            }
            
            if (c == null) {
                set.dispose();
                NullPointerException npe = new NullPointerException("A completable source is null");
                if (once.compareAndSet(false, true)) {
                    s.onError(npe);
                    return;
                } else {
                    RxJavaPlugins.onError(npe);
                }
            }
            
            c.subscribe(new CompletableSubscriber() {
                @Override
                public void onSubscribe(Disposable d) {
                    set.add(d);
                }

                @Override
                public void onError(Throwable e) {
                    set.dispose();
                    if (once.compareAndSet(false, true)) {
                        s.onError(e);
                    } else {
                        RxJavaPlugins.onError(e);
                    }
                }

                @Override
                public void onComplete() {
                    if (wip.decrementAndGet() == 0) {
                        if (once.compareAndSet(false, true)) {
                            s.onComplete();
                        }
                    }
                }
                
            });
        }
        
        if (wip.decrementAndGet() == 0) {
            if (once.compareAndSet(false, true)) {
                s.onComplete();
            }
        }
    }
}
