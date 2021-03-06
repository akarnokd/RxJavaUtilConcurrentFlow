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

package hu.akarnokd.rxjava2flow.internal.operators.single;

import java.util.function.Function;

import hu.akarnokd.rxjava2flow.Single.*;
import hu.akarnokd.rxjava2flow.disposables.Disposable;

public final class SingleOperatorMap<T, R> implements SingleOperator<R, T> {
    final Function<? super T, ? extends R> mapper;

    public SingleOperatorMap(Function<? super T, ? extends R> mapper) {
        this.mapper = mapper;
    }

    @Override
    public SingleSubscriber<? super T> apply(SingleSubscriber<? super R> t) {
        return new SingleSubscriber<T>() {
            @Override
            public void onSubscribe(Disposable d) {
                t.onSubscribe(d);
            }

            @Override
            public void onSuccess(T value) {
                R v;
                try {
                    v = mapper.apply(value);
                } catch (Throwable e) {
                    onError(e);
                    return;
                }
                
                t.onSuccess(v);
            }

            @Override
            public void onError(Throwable e) {
                t.onError(e);
            }
        };
    }
}
