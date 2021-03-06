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

package reactor.bus;

import reactor.bus.registry.Registration;
import reactor.bus.selector.Selector;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.fn.Supplier;

/**
 * Basic unit of event handling in Reactor.
 *
 * @author Jon Brisbin
 * @author Stephane Maldini
 * @author Andy Wilkinson
 */
public interface Observable {

	/**
	 * Are there any {@link Registration}s with {@link Selector Selectors} that match the given {@code key}.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 *
	 * @return {@literal true} if there are any matching {@literal Registration}s, {@literal false} otherwise
	 */
	boolean respondsToKey(Object key);

	/**
	 * Register a {@link reactor.fn.Consumer} to be triggered when a notification matches the given {@link
	 * Selector}.
	 *
	 * @param sel
	 * 		The {@literal Selector} to be used for matching
	 * @param consumer
	 * 		The {@literal Consumer} to be triggered
	 * @param <E>
	 * 		The type of the {@link Event}
	 *
	 * @return A {@link Registration} object that allows the caller to interact with the given mapping
	 */
	<E extends Event<?>> Registration<Consumer<E>> on(Selector sel, Consumer<E> consumer);

	/**
	 * Assign a {@link reactor.fn.Function} to receive an {@link Event} and produce a reply of the given type.
	 *
	 * @param sel
	 * 		The {@link Selector} to be used for matching
	 * @param fn
	 * 		The transformative {@link reactor.fn.Function} to call to receive an {@link Event}
	 * @param <E>
	 * 		The type of the {@link Event}
	 * @param <V>
	 * 		The type of the response data
	 *
	 * @return A {@link Registration} object that allows the caller to interact with the given mapping
	 */
	<E extends Event<?>, V> Registration<Consumer<E>> receive(Selector sel, Function<E, V> fn);

	/**
	 * Notify this component that an {@link Event} is ready to be processed.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 * @param ev
	 * 		The {@literal Event}
	 * @param <E>
	 * 		The type of the {@link Event}
	 *
	 * @return {@literal this}
	 */
	<E extends Event<?>> Observable notify(Object key, E ev);

	/**
	 * Notify this component that the given {@link reactor.fn.Supplier} can provide an event that's ready to be
	 * processed.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 * @param supplier
	 * 		The {@link reactor.fn.Supplier} that will provide the actual {@link Event}
	 * @param <S>
	 * 		The type of the {@link reactor.fn.Supplier}
	 *
	 * @return {@literal this}
	 */
	<S extends Supplier<? extends Event<?>>> Observable notify(Object key, S supplier);


	/**
	 * Notify this component of the given {@link Event} and register an internal {@link Consumer} that will take the
	 * output of a previously-registered {@link Function} and respond using the key set on the {@link Event}'s {@literal
	 * replyTo} property.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 * @param ev
	 * 		The {@literal Event}
	 * @param <E>
	 * 		The type of the {@link Event}
	 *
	 * @return {@literal this}
	 */
	<E extends Event<?>> Observable send(Object key, E ev);

	/**
	 * Notify this component that the given {@link Supplier} will provide an {@link Event} and register an internal {@link
	 * Consumer} that will take the output of a previously-registered {@link Function} and respond using the key set on
	 * the {@link Event}'s {@literal replyTo} property.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 * @param supplier
	 * 		The {@link Supplier} that will provide the actual {@link Event} instance
	 *
	 * @return {@literal this}
	 */
	<S extends Supplier<? extends Event<?>>> Observable send(Object key, S supplier);

	/**
	 * Notify this component of the given {@link Event} and register an internal {@link Consumer} that will take the
	 * output of a previously-registered {@link Function} and respond to the key set on the {@link Event}'s {@literal
	 * replyTo} property and will call the {@code notify} method on the given {@link Observable}.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 * @param ev
	 * 		The {@literal Event}
	 * @param replyTo
	 * 		The {@link Observable} on which to invoke the notify method
	 * @param <E>
	 * 		The type of the {@link Event}
	 *
	 * @return {@literal this}
	 */
	<E extends Event<?>> Observable send(Object key, E ev, Observable replyTo);

	/**
	 * Notify this component that the given {@link Supplier} will provide an {@link Event} and register an internal {@link
	 * Consumer} that will take the output of a previously-registered {@link Function} and respond to the key set on the
	 * {@link Event}'s {@literal replyTo} property and will call the {@code notify} method on the given {@link
	 * Observable}.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 * @param supplier
	 * 		The {@link Supplier} that will provide the actual {@link Event} instance
	 * @param replyTo
	 * 		The {@link Observable} on which to invoke the notify method
	 * @param <S>
	 * 		The type of the Supplier
	 *
	 * @return {@literal this}
	 */
	<S extends Supplier<? extends Event<?>>> Observable send(Object key, S supplier, Observable replyTo);

	/**
	 * Register the given {@link reactor.fn.Consumer} on an anonymous {@link reactor.bus.selector.Selector} and
	 * set the given event's {@code replyTo} property to the corresponding anonymous key, then register the consumer to
	 * receive replies from the {@link reactor.fn.Function} assigned to handle the given key.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 * @param ev
	 * 		The event to notify.
	 * @param reply
	 * 		The consumer to register as a reply handler.
	 * @param <REQ>
	 * 		The type of the request event.
	 * @param <RESP>
	 * 		The type of the response event.
	 *
	 * @return {@literal this}
	 */
	<REQ extends Event<?>, RESP extends Event<?>> Observable sendAndReceive(Object key, REQ ev, Consumer<RESP> reply);

	/**
	 * Register the given {@link reactor.fn.Consumer} on an anonymous {@link reactor.bus.selector.Selector} and
	 * set the event's {@code replyTo} property to the corresponding anonymous key, then register the consumer to receive
	 * replies from the {@link reactor.fn.Function} assigned to handle the given key.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 * @param supplier
	 * 		The supplier to supply the event.
	 * @param reply
	 * 		The consumer to register as a reply handler.
	 * @param <REQ>
	 * 		The type of the request event.
	 * @param <RESP>
	 * 		The type of the response event.
	 * @param <S>
	 * 		The type of the supplier.
	 *
	 * @return {@literal this}
	 */
	<REQ extends Event<?>, RESP extends Event<?>, S extends Supplier<REQ>> Observable sendAndReceive(Object key,
	                                                                                                 S supplier,
	                                                                                                 Consumer<RESP> reply);

	/**
	 * Notify this component that the consumers registered with a {@link Selector} that matches the {@code key} should be
	 * triggered with a {@literal null} input argument.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 *
	 * @return {@literal this}
	 */
	Observable notify(Object key);

	/**
	 * Create an optimized path for publishing notifications to the given key.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 *
	 * @return a {@link Consumer} to invoke with the {@link Event Events} to publish
	 */
	<T> Consumer<Event<T>> prepare(Object key);

	/**
	 * Notify the key with all any accepted iterable group of events by the returned {@link Consumer}. The implementation
	 * will take care of reducing the consumer selection to one per batch. The candidate consumers are selected with the
	 * key {@param key}, possibly on each batch to refresh the result list.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 *
	 * @return a {@link Consumer} to invoke with the {@link Event Events} to publish
	 */
	<T> Consumer<Iterable<Event<T>>> batchNotify(Object key);

	/**
	 * Notify the key with all any accepted iterable group of events by the returned {@link Consumer}. The implementation
	 * will take care of reducing the consumer selection to one per batch. The candidate consumers are selected with the
	 * key {@param key}, possibly on each batch to refresh the result list.
	 *
	 * @param key
	 * 		The key to be matched by {@link Selector Selectors}
	 * @param consumer
	 * 		The consumer to trigger after batch completion
	 *
	 * @return a {@link Consumer} to invoke with the {@link Event Events} to publish
	 */
	<T> Consumer<Iterable<Event<T>>> batchNotify(Object key, Consumer<Void> consumer);

}
