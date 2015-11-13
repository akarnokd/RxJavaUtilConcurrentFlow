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
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.Flow.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.*;
import org.mockito.InOrder;

import hu.akarnokd.rxjava2flow.*;
import hu.akarnokd.rxjava2flow.exceptions.TestException;
import hu.akarnokd.rxjava2flow.internal.subscriptions.*;
import hu.akarnokd.rxjava2flow.schedulers.Schedulers;
import hu.akarnokd.rxjava2flow.subjects.PublishSubject;
import hu.akarnokd.rxjava2flow.subscribers.TestSubscriber;

public class OperatorTakeTest {

    @Test
    public void testTake1() {
        Observable<String> w = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        Observable<String> take = w.take(2);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        take.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTake2() {
        Observable<String> w = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        Observable<String> take = w.take(1);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        take.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTakeWithError() {
        Observable.fromIterable(Arrays.asList(1, 2, 3)).take(1)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        }).toBlocking().single();
    }

    @Test
    public void testTakeWithErrorHappeningInOnNext() {
        Observable<Integer> w = Observable.fromIterable(Arrays.asList(1, 2, 3))
                .take(2).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        });

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        w.subscribe(observer);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(any(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTakeWithErrorHappeningInTheLastOnNext() {
        Observable<Integer> w = Observable.fromIterable(Arrays.asList(1, 2, 3)).take(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        });

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        w.subscribe(observer);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(any(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTakeDoesntLeakErrors() {
        Observable<String> source = Observable.create(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                observer.onNext("one");
                observer.onError(new Throwable("test failed"));
            }
        });

        Subscriber<String> observer = TestHelper.mockSubscriber();

        source.take(1).subscribe(observer);

        verify(observer).onSubscribe((Subscription)notNull());
        verify(observer, times(1)).onNext("one");
        // even though onError is called we take(1) so shouldn't see it
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    @Ignore("take(0) is now empty() and doesn't even subscribe to the original source")
    public void testTakeZeroDoesntLeakError() {
        final AtomicBoolean subscribed = new AtomicBoolean(false);
        BooleanSubscription bs = new BooleanSubscription();
        Observable<String> source = Observable.create(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                subscribed.set(true);
                observer.onSubscribe(bs);
                observer.onError(new Throwable("test failed"));
            }
        });

        Subscriber<String> observer = TestHelper.mockSubscriber();

        source.take(0).subscribe(observer);
        assertTrue("source subscribed", subscribed.get());
        assertTrue("source unsubscribed", bs.isCancelled());

        verify(observer, never()).onNext(anyString());
        // even though onError is called we take(0) so shouldn't see it
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testUnsubscribeAfterTake() {
        TestObservableFunc f = new TestObservableFunc("one", "two", "three");
        Observable<String> w = Observable.create(f);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        
        Observable<String> take = w.take(1);
        take.subscribe(observer);

        // wait for the Observable to complete
        try {
            f.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        System.out.println("TestObservable thread finished");
        verify(observer).onSubscribe((Subscription)notNull());
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, times(1)).onComplete();
        // FIXME no longer assertable
//        verify(s, times(1)).unsubscribe();
        verifyNoMoreInteractions(observer);
    }

    @Test(timeout = 2000)
    public void testUnsubscribeFromSynchronousInfiniteObservable() {
        final AtomicLong count = new AtomicLong();
        INFINITE_OBSERVABLE.take(10).subscribe(new Consumer<Long>() {

            @Override
            public void accept(Long l) {
                count.set(l);
            }

        });
        assertEquals(10, count.get());
    }

    @Test(timeout = 2000)
    public void testMultiTake() {
        final AtomicInteger count = new AtomicInteger();
        Observable.create(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                BooleanSubscription bs = new BooleanSubscription();
                s.onSubscribe(bs);
                for (int i = 0; !bs.isCancelled(); i++) {
                    System.out.println("Emit: " + i);
                    count.incrementAndGet();
                    s.onNext(i);
                }
            }

        }).take(100).take(1).toBlocking().forEach(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                System.out.println("Receive: " + t1);

            }

        });

        assertEquals(1, count.get());
    }

    private static class TestObservableFunc implements Publisher<String> {

        final String[] values;
        Thread t = null;

        public TestObservableFunc(String... values) {
            this.values = values;
        }

        @Override
        public void subscribe(final Subscriber<? super String> observer) {
            observer.onSubscribe(EmptySubscription.INSTANCE);
            System.out.println("TestObservable subscribed to ...");
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        observer.onComplete();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
        }
    }

    private static Observable<Long> INFINITE_OBSERVABLE = Observable.create(new Publisher<Long>() {

        @Override
        public void subscribe(Subscriber<? super Long> op) {
            BooleanSubscription bs = new BooleanSubscription();
            op.onSubscribe(bs);
            long l = 1;
            while (!bs.isCancelled()) {
                op.onNext(l++);
            }
            op.onComplete();
        }

    });
    
    @Test(timeout = 2000)
    public void testTakeObserveOn() {
        Subscriber<Object> o = TestHelper.mockSubscriber();
        TestSubscriber<Object> ts = new TestSubscriber<>(o);
        
        INFINITE_OBSERVABLE.onBackpressureDrop()
        .observeOn(Schedulers.newThread()).take(1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        
        verify(o).onNext(1L);
        verify(o, never()).onNext(2L);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testProducerRequestThroughTake() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Observable.create(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                    @Override
                    public void cancel() {
                        
                    }
                });
            }

        }).take(3).subscribe(ts);
        // FIXME take triggers fast-path
        assertEquals(Long.MAX_VALUE, requested.get());
    }
    
    @Test
    public void testProducerRequestThroughTakeIsModified() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Observable.create(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                    @Override
                    public void cancel() {
                        
                    }
                });
            }

        }).take(1).subscribe(ts);
        //FIXME take triggers fast path if downstream requests more than the limit
        assertEquals(Long.MAX_VALUE, requested.get());
    }
    
    @Test
    public void testInterrupt() throws InterruptedException {
        final AtomicReference<Object> exception = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        Observable.just(1).subscribeOn(Schedulers.computation()).take(1)
        .subscribe(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    exception.set(e);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }

        });

        latch.await();
        assertNull(exception.get());
    }
    
    @Test
    public void testDoesntRequestMoreThanNeededFromUpstream() throws InterruptedException {
        final AtomicLong requests = new AtomicLong();
        TestSubscriber<Long> ts = new TestSubscriber<>((Long)null);
        Observable.interval(100, TimeUnit.MILLISECONDS)
            //
            .doOnRequest(new LongConsumer() {
                @Override
                public void accept(long n) {
                    System.out.println(n);
                    requests.addAndGet(n);
            }})
            //
            .take(2)
            //
            .subscribe(ts);
        Thread.sleep(50);
        ts.request(1);
        ts.request(1);
        ts.request(1);
        ts.awaitTerminalEvent();
        ts.assertComplete();
        ts.assertNoErrors();
        // FIXME take triggers fast-path and the addition above turns into Long.MIN_VALUE
        assertEquals(Long.MIN_VALUE, requests.get());
    }
    
    @Test
    public void takeFinalValueThrows() {
        Observable<Integer> source = Observable.just(1).take(1);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };
        
        source.safeSubscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }
    
    @Test
    public void testReentrantTake() {
        PublishSubject<Integer> source = PublishSubject.create();
        
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        
        source.take(1).doOnNext(v -> source.onNext(2)).subscribe(ts);
        
        source.onNext(1);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }
}