/**
 * Copyright 2015 David Karnok and Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.akarnokd.rxjava2flow.nbp;

import static hu.akarnokd.rxjava2flow.NbpObservable.combineLatest;

import java.util.function.*;

import org.junit.*;

import hu.akarnokd.rxjava2flow.NbpObservable;
import hu.akarnokd.rxjava2flow.nbp.NbpCovarianceTest.*;
import hu.akarnokd.rxjava2flow.subjects.nbp.NbpBehaviorSubject;

public class NbpCombineLatestTests {
    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfCombineLatest() {
        NbpObservable<HorrorMovie> horrors = NbpObservable.just(new HorrorMovie());
        NbpObservable<CoolRating> ratings = NbpObservable.just(new CoolRating());

        NbpObservable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).toBlocking().forEach(action);
        NbpObservable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).toBlocking().forEach(action);
        NbpObservable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).toBlocking().forEach(extendedAction);
        NbpObservable.<Media, Rating, Result> combineLatest(horrors, ratings, combine).toBlocking().forEach(action);
        NbpObservable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).toBlocking().forEach(action);

        NbpObservable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine);
    }

    BiFunction<Media, Rating, ExtendedResult> combine = (m, r) -> new ExtendedResult();

    Consumer<Result> action = t1 -> System.out.println("Result: " + t1);

    Consumer<ExtendedResult> extendedAction = t1 -> System.out.println("Result: " + t1);

    @Ignore
    @Test
    public void testNullEmitting() throws Exception {
        // FIXME this is no longer allowed
        NbpObservable<Boolean> nullNbpObservable = NbpBehaviorSubject.createDefault((Boolean) null);
        NbpObservable<Boolean> nonNullNbpObservable = NbpBehaviorSubject.createDefault(true);
        NbpObservable<Boolean> combined =
                combineLatest(nullNbpObservable, nonNullNbpObservable, new BiFunction<Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean apply(Boolean bool1, Boolean bool2) {
                        return bool1 == null ? null : bool2;
                    }
                });
        combined.subscribe(aBoolean -> Assert.assertNull(aBoolean));
    }
}