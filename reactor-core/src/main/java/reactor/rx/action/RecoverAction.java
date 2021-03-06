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
package reactor.rx.action;

import org.reactivestreams.Subscriber;
import reactor.bus.selector.ClassSelector;
import reactor.core.Dispatcher;
import reactor.rx.subscription.PushSubscription;

/**
 * @author Stephane Maldini
 * @since 1.1
 */
public class RecoverAction<T, E extends Throwable> extends Action<T, E> {

	private final ClassSelector selector;
	private       E             lastError;

	public RecoverAction(Dispatcher dispatcher, ClassSelector selector) {
		super(dispatcher);
		this.selector = selector;
	}

	@Override
	protected PushSubscription<E> createSubscription(final Subscriber<? super E> subscriber, boolean reactivePull) {
		if (lastError != null) {
			return new PushSubscription<E>(this, subscriber) {
				@Override
				public void request(long elements) {
					subscriber.onNext(lastError);
				}
			};
		} else {
			return super.createSubscription(subscriber, reactivePull);
		}
	}

	@Override
	public void onNext(T ev) {
		//IGNORE
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void doError(Throwable cause) {
		if (selector.matches(cause.getClass())) {
			lastError = (E) cause;
			broadcastNext((E) cause);
		} else {
			super.doError(cause);
		}
	}

	@Override
	protected void doNext(Object ev) {
		//ignore
	}

	@Override
	public String toString() {
		return super.toString() + "{" +
				"catch-type=" + selector.getObject() + ", " +
				'}';
	}
}
