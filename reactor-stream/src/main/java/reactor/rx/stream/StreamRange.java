/*
 * Copyright (c) 2011-2014 Pivotal Software, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package reactor.rx.stream;

import org.reactivestreams.Subscriber;
import reactor.core.publisher.FluxFactory;
import reactor.core.subscriber.SubscriberWithContext;
import reactor.core.support.ReactiveState;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.rx.Stream;
import reactor.rx.Streams;

/**
 * A Stream that emits N {@link java.lang.Long} from the inclusive start value defined to the inclusive end and then
 * complete.
 * <p>
 * Since the stream retains the boundaries in a final field, any {@link org.reactivestreams.Subscriber}
 * will replay all the range. This is a "Cold" stream.
 * <p>
 * Create such stream with the provided factory, E.g.:
 * <pre>
 * {@code
 * Streams.range(1, 10000).consume(
 *    log::info,
 *    log::error,
 *    (-> log.info("complete"))
 * )
 * }
 * </pre>
 * <pre>
 * {@code
 * Will log:
 * 1
 * 2
 * 3
 * 4
 * complete
 * }
 * </pre>
 *
 * @author Stephane Maldini
 */
public final class StreamRange {

	private StreamRange() {
	}

	/**
	 * Create a Range Stream Publisher
	 *
	 * @param min
	 * @param count
	 * @return
	 */
	public static Stream<Integer> create(final int min, final int count) {


		if(count < 0){
			throw new IllegalArgumentException("Count can not be negative : "+count);
		}
		else if(count == 0) {
			return Streams.empty();
		}
		else if ( count == 1 ){
			return Streams.just(min);
		}
		else if(min + count > Integer.MAX_VALUE){
			throw new IllegalArgumentException("start + count can not exceed Integer.MAX_VALUE");
		}


		return Streams.wrap(FluxFactory.create(RangeSequence.INSTANCE, new Function<Subscriber<? super Integer>, Range>() {
			  @Override
			  public Range apply(Subscriber<? super Integer> subscriber) {
				  return new Range(min, count);
			  }
		  })
		);
	}

	private final static class Range implements ReactiveState.ActiveUpstream, ReactiveState.Named {
		final int count;
		final int start;

		int cursor;

		public Range(int start, int count) {
			this.count = count;
			this.start = start;
			cursor = 0;
		}

		@Override
		public String toString() {
			return "{" +
			  "cursor=" + cursor + "" + (count > 0 ? "[" + 100 * (cursor - 1) / count + "%]" : "") +
			  ", start=" + start + ", count=" + count + "}";
		}

		@Override
		public String getName() {
			return "Range["+(start + cursor - 1)+".."+(start+count - 1)+"]";
		}

		@Override
		public boolean isStarted() {
			return cursor > 0;
		}

		@Override
		public boolean isTerminated() {
			return cursor == count;
		}
	}

	private static class RangeSequence
			implements Consumer<SubscriberWithContext<Integer, Range>> {

		private final static RangeSequence INSTANCE = new RangeSequence();

		@Override
		public void accept(SubscriberWithContext<Integer, Range> subscriber) {
			Range range = subscriber.context();

			if (range.cursor < range.count) {
				subscriber.onNext(range.start + range.cursor++);
			}
			if (range.cursor >= range.count) {
				subscriber.onComplete();
			}
		}


	}
}
