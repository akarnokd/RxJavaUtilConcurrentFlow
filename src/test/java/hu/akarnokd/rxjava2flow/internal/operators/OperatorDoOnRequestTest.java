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

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

import org.junit.Test;

import hu.akarnokd.rxjava2flow.Observable;
import hu.akarnokd.rxjava2flow.Observer;

public class OperatorDoOnRequestTest {

    @Test
    public void testUnsubscribeHappensAgainstParent() {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Observable.just(1)
        //
                .doOnCancel(new Runnable() {
                    @Override
                    public void run() {
                        unsubscribed.set(true);
                    }
                })
                //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        // do nothing
                    }
                })
                //
                .subscribe();
        assertTrue(unsubscribed.get());
    }

    @Test
    public void testDoRequest() {
        final List<Long> requests = new ArrayList<>();
        Observable.range(1, 5)
        //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        requests.add(n);
                    }
                })
                //
                .subscribe(new Observer<Integer>() {

                    @Override
                    public void onStart() {
                        request(3);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                        request(t);
                    }
                });
        assertEquals(Arrays.asList(3L,1L,2L,3L,4L,5L), requests);
    }

}