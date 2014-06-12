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
package reactor.rx.action;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.event.dispatch.Dispatcher;
import reactor.event.dispatch.SingleThreadDispatcher;
import reactor.rx.StreamSubscription;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Stephane Maldini
 * @since 1.1
 */
public class MergeAction<O> extends Action<O, O> {

	final AtomicInteger          runningComposables;
	final Subscription[]         subscriptions;
	final Action<O, ?>           processingAction;
	final SingleThreadDispatcher mergedDispatcher;

	private final static Publisher[] EMPTY_PIPELINE = new Publisher[0];

	@SuppressWarnings("unchecked")
	public MergeAction(Dispatcher dispatcher) {
		this(dispatcher, null, EMPTY_PIPELINE);
	}

	public MergeAction(Dispatcher dispatcher, Action<O, ?> processingAction, Publisher<O>... composables) {
		super(dispatcher);
		this.mergedDispatcher = SingleThreadDispatcher.class.isAssignableFrom(dispatcher.getClass()) ?
				(SingleThreadDispatcher)dispatcher : null;

		int length = composables.length;
		this.processingAction = processingAction;
		this.subscriptions = new Subscription[length];

		if (composables != null && length > 0) {
			this.runningComposables = new AtomicInteger(processingAction == null ? length + 1 : length);
			Publisher<O> composable;
			for (int i = 0; i < length; i++) {
				final int pos = i;
				composable = composables[i];
				composable.subscribe(new Action<O, O>(dispatcher) {
					@Override
					protected void doSubscribe(Subscription subscription) {
						subscriptions[pos] = subscription;
					}

					@Override
					protected void doFlush() {
						MergeAction.this.doFlush();
					}

					@Override
					protected void doComplete() {
						MergeAction.this.doComplete();
					}

					@Override
					protected void doNext(O ev) {
						MergeAction.this.doNext(ev);
					}

					@Override
					protected void doError(Throwable ev) {
						MergeAction.this.doError(ev);
					}
				});

				if (processingAction != null) {
					processingAction.doSubscribe(createSubscription(processingAction));

				}
			}
		} else {
			this.runningComposables = new AtomicInteger(0);
		}
	}

	@Override
	public void onNext(O ev) {
		if(mergedDispatcher != null && mergedDispatcher.getRemainingSlots() <= 0){
			mergedDispatcher.scheduleWithinLastExecutedDispatch(this, ev, null, null, ROUTER, this);
		}else{
			super.onNext(ev);
		}
	}

	public void addPublisher(Publisher<O> publisher) {
		runningComposables.incrementAndGet();
		Action<O, Void> inlineMerge = new Action<O, Void>(getDispatcher(), batchSize) {
			@Override
			protected void doSubscribe(Subscription s) {
				available();
			}

			@Override
			protected void doFlush() {
				MergeAction.this.doFlush();
			}

			@Override
			protected void doComplete() {
				MergeAction.this.doComplete();
			}

			@Override
			protected void doNext(O ev) {
				MergeAction.this.doNext(ev);
				available();
			}

			@Override
			protected void doError(Throwable ev) {
				MergeAction.this.doError(ev);
			}

		};
		publisher.subscribe(inlineMerge);
	}

	@Override
	protected StreamSubscription<O> createSubscription(Subscriber<O> subscriber) {
		if (subscriptions.length > 0) {
			return new StreamSubscription<O>(this, subscriber) {
				@Override
				public void request(int elements) {
					super.request(elements * (subscriptions.length + 1));
					for (Subscription subscription : subscriptions) {
						if (subscription != null) {
							subscription.request(elements);
						}
					}
					requestUpstream(capacity, buffer.isComplete(), elements);
				}

				@Override
				public void cancel() {
					super.cancel();
					for (Subscription subscription : subscriptions) {
						if (subscription != null) {
							subscription.cancel();
						}
					}
				}
			};
		} else {
			return super.createSubscription(subscriber);
		}
	}

	@Override
	protected void doNext(O ev) {
		if (processingAction != null) {
			processingAction.doNext(ev);
		} else {
			broadcastNext(ev);
		}
	}

	@Override
	protected void doSubscribe(Subscription subscription) {
		if (processingAction != null) {
			processingAction.onSubscribe(subscription);
		} else {
			super.doSubscribe(subscription);
		}
	}

	@Override
	protected void doFlush() {
		if (processingAction != null) {
			processingAction.doFlush();
		} else {
			super.doFlush();
		}
	}

	@Override
	protected void doError(Throwable ev) {
		if (processingAction != null) {
			processingAction.doError(ev);
		} else {
			super.doError(ev);
		}
	}

	@Override
	protected void doComplete() {
		if (runningComposables.decrementAndGet() == 0) {
			if (processingAction == null) {
				broadcastComplete();
			} else {
				processingAction.onComplete();
			}

		}
	}

	@Override
	public String toString() {
		return super.toString() +
				"{runningComposables=" + runningComposables +
				'}';
	}
}