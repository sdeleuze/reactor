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
package reactor.event.registry;

import reactor.function.Consumer;

import java.util.Iterator;

/**
 * @author Stephane Maldini
 * @since 1.1
 */
public class LinkedRegistrations<T> implements Consumer<Consumer<Registration<? extends T>>>,
		Iterable<Registration<? extends T>> {

	private final LinkedRegistrations<T>    root;
	private final Registration<? extends T> registration;

	private LinkedRegistrations<T> nextRegistrationNode;
	private int size = 0;

	LinkedRegistrations(Registration<? extends T> registration,
	                    LinkedRegistrations<T> root) {
		this.nextRegistrationNode = null;
		this.root = root == null ? this : root;
		this.registration = registration;
	}

	LinkedRegistrations(Registration<? extends T> registration) {
		this(registration, null);
	}

	LinkedRegistrations<T> append(Registration<? extends T> registration) {
		nextRegistrationNode = new LinkedRegistrations<T>(registration, root);
		root.size++;
		return nextRegistrationNode;
	}

	public void accept(Consumer<Registration<? extends T>> consumer) {
		LinkedRegistrations<T> node = root;
		while (node != null) {
			if (node.registration != null) {
				consumer.accept(node.registration);
			}
			node = node.nextRegistrationNode;
		}
		consumer.accept(null);
	}

	public void cancel() {
		if (registration != null) {
			registration.cancel();
		}
	}

	@Override
	public Iterator<Registration<? extends T>> iterator() {
		final LinkedRegistrations<T> thiz = this;
		return new Iterator<Registration<? extends T>>() {
			private LinkedRegistrations<T> node = thiz.root;

			@Override
			public boolean hasNext() {
				return node != null && node.registration != null;
			}

			@Override
			public Registration<? extends T> next() {
				Registration<? extends T> reg = node.registration;
				node = node.nextRegistrationNode;
				return reg;
			}
		};
	}

	public int size() {
		return root.size;
	}
}
