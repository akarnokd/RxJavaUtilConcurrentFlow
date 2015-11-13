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

package hu.akarnokd.rxjava2flow;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.util.concurrent.Flow.*;

import org.mockito.Mockito;

import hu.akarnokd.rxjava2flow.NbpObservable.NbpSubscriber;

/**
 * Common methods for helping with tests from 1.x mostly.
 */
public enum TestHelper {
    ;
    /**
     * Mocks a subscriber and prepares it to request Long.MAX_VALUE.
     * @return the mocked subscriber
     */
    @SuppressWarnings("unchecked")
    public static <T> Subscriber<T> mockSubscriber() {
        Subscriber<T> w = mock(Subscriber.class);
        
        Mockito.doAnswer(a -> {
            Subscription s = a.getArgumentAt(0, Subscription.class);
            s.request(Long.MAX_VALUE);
            return null;
        }).when(w).onSubscribe(any());
        
        return w;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> NbpSubscriber<T> mockNbpSubscriber() {
        return mock(NbpSubscriber.class);
    }
}
