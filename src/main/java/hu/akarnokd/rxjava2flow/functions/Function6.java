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

package hu.akarnokd.rxjava2flow.functions;

import java.util.function.Function;

@FunctionalInterface
public interface Function6<T1, T2, T3, T4, T5, T6, R> extends Function<Object[], R> {
    R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);
    
    @Override
    @SuppressWarnings("unchecked")
    default R apply(Object[] a) {
        if (a.length != 6) {
            throw new IllegalArgumentException("Array of size 6 expected but got " + a.length);
        }
        return apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4], (T6)a[5]);
    }
}
