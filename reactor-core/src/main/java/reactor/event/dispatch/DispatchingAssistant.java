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
package reactor.event.dispatch;

import reactor.core.Observable;
import reactor.event.Event;
import reactor.function.Consumer;
import reactor.timer.Timer;

/**
 * A {@code DispatchingAssistant} is used to provide hints to the underlying dispatching logic. Dispatching might
 * take various forms but a fairly common pattern if the consumer is not synchronously called is to use a shared
 * structure (RingBuffer, BlockingQueue...). A {@code DispatchingAssitant} can suggest to wait for an available slot
 * from such structure to be available with {@link DispatchingAssistant#NEW_ITERATION}.
 *
 * Single threaded dispatchers might take advantage of tail recursion, which means that a recursive {@link
 * Observable#notify(Object, reactor.event.Event)} will queue the dispatching before completing the current event
 * loop iteration. To force this behavior, one can make use of {@link DispatchingAssistant#NEXT_ITERATION} option.
 *
 * Eventually, dispatching can be guarded by the {@link DispatchingAssistant#recover(Object,
 * reactor.event.Event, reactor.function.Consumer)} implementation when {@link DispatchingAssistant#recoverPredicate
 * (boolean, Object, reactor.event.Event)} returns true. This offers a range of recovery options such as delaying,
 * short-circuiting, logging, retrying, throttle, scale up/down etc...
 *
 * A {@code DispatchingAssistant} is injected with the current dispatching context before
 *
 * @author Stephane Maldini
 * @since 1.1.0
 */
public class DispatchingAssistant implements Cloneable{

	public final static byte NEW_ITERATION  = 1;
	public final static byte NEXT_ITERATION = 2;
	public final static byte TRY_DISPATCH   = 3;

	/**
	 * a static dispatching strategy commonly used to enforce tail recursion. E.G. when an observable consumes an event
	 * that is forwarded to a different observable which is itself forwarding back the event to the initial observable.
	 * Such A->B->A flow can cause dispatchers to deadlock on their task queue, in that case the consumer B should
	 * notify A with this dispatching assistant to tell it to enqueue the task for processing at the end of the current
	 * loop iteration.
	 */
	public final static DispatchingAssistant NEXT_ITERATION_ASSISTANT = new DispatchingAssistant(NEXT_ITERATION);

	final private byte  dispatchingOptions;
	final private Timer timer;

	private Dispatcher dispatcher;

	public DispatchingAssistant() {
		this(TRY_DISPATCH);
	}

	public DispatchingAssistant(byte dispatchingOptions) {
		this(dispatchingOptions, null);
	}

	public DispatchingAssistant(byte dispatchingOptions, Timer timer) {
		this.dispatchingOptions = dispatchingOptions;
		this.timer = timer;
	}

	public <E extends Event<?>> void recover(Object key, E event, Consumer<E> onComplete) {
	}

	public <E extends Event<?>> boolean recoverPredicate(boolean fullCapacity, Object key, E event) {
		return fullCapacity;
	}

	final public byte dispatchingOptions() {
		return dispatchingOptions;
	}


	final public Boolean isDispatchOnNextIteration() {
		if ((dispatchingOptions & NEXT_ITERATION) == NEXT_ITERATION) {
			return true;
		} else if ((dispatchingOptions & NEW_ITERATION) == NEW_ITERATION) {
			return false;
		}
		return null;
	}

	final public boolean isSafeDispatch() {
		return (dispatchingOptions & TRY_DISPATCH) == TRY_DISPATCH;
	}

	final void dispatcher(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	final protected Timer timer() {
		return timer;
	}

	final protected void retry() {

	}

	final protected Dispatcher dispatcher() {
		return dispatcher;
	}

	@Override
	public DispatchingAssistant clone() throws CloneNotSupportedException {
		return (DispatchingAssistant) super.clone();
	}
}
