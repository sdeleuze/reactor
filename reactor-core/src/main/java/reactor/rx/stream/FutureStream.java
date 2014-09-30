/*
 * Copyright (c) 2011-2013 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.rx.stream;

import org.reactivestreams.Subscriber;
import reactor.rx.Stream;
import reactor.rx.subscription.ReactiveSubscription;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A Stream that emits a result of a {@link java.util.concurrent.Future} and then complete.
 * <p>
 * Since the stream retains the future reference in a final field, any {@link this#subscribe(org.reactivestreams.Subscriber)}
 * will replay the {@link java.util.concurrent.Future#get()}
 * <p>
 * Create such stream with the provided factory, E.g.:
 * {@code
 * Streams.just(someFuture).consume(
 *log::info,
 *log::error,
 * (-> log.info("complete"))
 * )
 * }
 * <p>
 * Will log:
 * 1
 * 2
 * 3
 * 4
 * complete
 *
 * @author Stephane Maldini
 */
public final class FutureStream<T> extends Stream<T> {

	private final Future<? extends T> future;
	private final long                time;
	private final TimeUnit            unit;

	public FutureStream(Future<? extends T> future) {
		this(future, 0, TimeUnit.SECONDS);
	}

	public FutureStream(Future<? extends T> future,
	                    long time,
	                    TimeUnit unit) {
		this.future = future;
		this.time = time;
		this.unit = unit;
	}

	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		subscriber.onSubscribe(new ReactiveSubscription<T>(this, subscriber) {

			@Override
			public void request(long elements) {
				super.request(elements);

				if (buffer.isComplete()) return;

				try {
					T result = unit == null ? future.get() : future.get(time, unit);

					buffer.complete();

					onNext(result);
					onComplete();

				} catch (Throwable e) {
					onError(e);
				}
			}
		});
	}
}