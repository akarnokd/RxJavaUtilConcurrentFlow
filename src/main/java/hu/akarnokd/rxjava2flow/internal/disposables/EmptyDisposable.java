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

package hu.akarnokd.rxjava2flow.internal.disposables;

import hu.akarnokd.rxjava2flow.NbpObservable.NbpSubscriber;
import hu.akarnokd.rxjava2flow.disposables.Disposable;

public enum EmptyDisposable implements Disposable {
    INSTANCE
    ;
    
    @Override
    public void dispose() {
        // no-op
    }
    
    public static void complete(NbpSubscriber<?> s) {
        s.onSubscribe(INSTANCE);
        s.onComplete();
    }
    
    public static void error(Throwable e, NbpSubscriber<?> s) {
        s.onSubscribe(INSTANCE);
        s.onError(e);
    }
}
