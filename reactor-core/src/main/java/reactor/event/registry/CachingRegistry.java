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

import reactor.event.lifecycle.Pausable;
import reactor.event.selector.ObjectSelector;
import reactor.event.selector.Selector;
import reactor.event.selector.Selectors;
import reactor.function.Consumer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An optimized selectors registry working with a L1 Cache and spare use of reentrant locks.
 * A Caching registry contains 3 different cache level always hit in this order:
 * - prime cache : if the selected key or registered selector object is exactly of type Object or {@link
 * Selectors.AnonymousKey},
 * this is eagerly updated on registration/unregistration without needing to reset its complete state,
 * thanks to the direct mapping between an Object.hashcode and the map key. This greatly optimizes composables which
 * make use of anonymous selector.
 * - cache : classic cache, filled after a first select miss using the key hashcode,
 * totally cleared on new registration
 * - full collection : where the registrations always live, acting like a pool. Iterated completely when cache miss.
 * Registration array grows for 75% of its current size when there is not enough pre-allocated memory
 *
 * @param <T> the type of Registration held by this registry
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public class CachingRegistry<T> implements Registry<T> {

	private static final Selector NO_MATCH = new ObjectSelector<Void>(
			null) {
		@Override
		public boolean matches(Object key) {
			return false;
		}
	};

	private final LinkedRegistrations<T>               EMPTY          = new LinkedRegistrations<T>(null);
	private final ReentrantLock                        cacheLock      = new ReentrantLock();
	private final ReentrantLock                        primeCacheLock = new ReentrantLock();
	private final ReentrantLock                        regLock        = new ReentrantLock();
	private final Map<Integer, LinkedRegistrations<T>> cache          = new HashMap<Integer,
			LinkedRegistrations<T>>();
	private final Map<Integer, LinkedRegistrations<T>> primeCache     = new HashMap<Integer,
			LinkedRegistrations<T>>();

	private volatile LinkedRegistrations<T> registrations;

	public CachingRegistry(LinkedRegistrations<T> registrations) {
		this.registrations = registrations;
	}

	public CachingRegistry() {
		this(null);
	}

	@Override
	public <V extends T> Registration<V> register(Selector sel, V obj) {
		Registration<V> reg = new SimpleRegistration<V>(sel, obj);

		regLock.lock();
		try {
			registrations = registrations == null ? new LinkedRegistrations<T>(reg) : registrations.append(reg);
		} finally {
			regLock.unlock();
		}

		// prime cache for anonymous Objects, Strings, etc...in an ObjectSelector
		if (Object.class.equals(sel.getObject().getClass()) || Selectors.AnonymousKey.class.equals(sel.getObject().getClass())) {
			int hashCode = sel.getObject().hashCode();
			primeCacheLock.lock();
			try {
				LinkedRegistrations<T> node = primeCache.get(hashCode);
				if (null == node) {
					node = new LinkedRegistrations<T>(reg);
				} else {
					node = node.append(reg);
				}
				primeCache.put(hashCode, node);
			} finally {
				primeCacheLock.unlock();
			}
		} else {
			cacheLock.lock();
			try {
				cache.clear();
			} finally {
				cacheLock.unlock();
			}
		}

		return reg;
	}

	@Override
	public boolean unregister(Object key) {
		regLock.lock();
		try {
			if (key.getClass().equals(Object.class)) {
				primeCacheLock.lock();
				try {
					LinkedRegistrations<T> registrations = primeCache.remove(key.hashCode());
					registrations.accept(new Consumer<Registration<? extends T>>() {
						@Override
						public void accept(Registration<? extends T> reg) {
							reg.cancel();
						}
					});
					return registrations != null;
				} finally {
					primeCacheLock.unlock();
				}
			} else {
				cacheLock.lock();
				try {
					final AtomicBoolean updated = new AtomicBoolean();
					select(key).accept(new Consumer<Registration<? extends T>>() {
						@Override
						public void accept(Registration<? extends T> registration) {
							if (registration != null) {
								registration.cancel();
								updated.set(true);
							}
						}
					});
					cache.remove(key.hashCode());
					return updated.get();
				} finally {
					cacheLock.unlock();
				}
			}
		} finally {
			regLock.unlock();
		}
	}


	@Override
	public LinkedRegistrations<T> select(final Object key) {
		if (null == key || registrations == null) {
			return EMPTY;
		}
		final int hashCode = key.hashCode();
		LinkedRegistrations<T> regs;
		//Todo do we need to match generic Objects too ?
		if (key.getClass().equals(Selectors.AnonymousKey.class) || key.getClass().equals(Object.class)) {
			primeCacheLock.lock();
			try {
				regs = primeCache.get(hashCode);
			} finally {
				primeCacheLock.unlock();
			}
			if (null != regs) {
				return regs;
			} else {
				return EMPTY;
			}
		}

		cacheLock.lock();
		try {
			regs = cache.get(hashCode);
			if (null != regs) {
				return regs;
			}

			// cache miss
			cacheMiss(key);

			final AtomicReference<LinkedRegistrations<T>> refRegs = new AtomicReference<LinkedRegistrations<T>>(
					null
			);

			registrations.accept(new Consumer<Registration<? extends T>>() {
				LinkedRegistrations<T> regs = refRegs.get();

				@Override
				public void accept(Registration<? extends T> reg) {
					if (reg == null) {
						cache.put(hashCode, regs);
						refRegs.set(regs);
					} else if (!reg.isCancelled() && !reg.isPaused() && reg.getSelector().matches(key)) {
						regs =  regs == null ? new LinkedRegistrations<T>(reg) : regs.append(reg);
					}
				}
			});

			regs = refRegs.get();

		} finally {
			cacheLock.unlock();
		}
		return regs;
	}

	@Override
	public void clear() {
		regLock.lock();
		try {
			cacheLock.lock();
			try {
				for (Registration registration : registrations) {
					if (registration != null) {
						registration.cancel();
					}
				}
				cache.clear();
			} finally {
				cacheLock.unlock();
			}
		} finally {
			regLock.unlock();
		}
	}

	@Override
	public Iterator<Registration<? extends T>> iterator() {
		return registrations.iterator();
	}

	protected void cacheMiss(Object key) {
	}


	private static class SimpleRegistration<T> implements Registration<T> {
		private final Selector selector;
		private final T        object;
		private final boolean  lifecycle;

		private volatile boolean cancelled      = false;
		private volatile boolean cancelAfterUse = false;
		private volatile boolean paused         = false;

		private SimpleRegistration(Selector selector, T object) {
			this.selector = selector;
			this.object = object;
			this.lifecycle = Pausable.class.isAssignableFrom(object.getClass());
		}

		@Override
		public Selector getSelector() {
			return (!cancelled ? selector : NO_MATCH);
		}

		@Override
		public T getObject() {
			return (!cancelled && !paused ? object : null);
		}

		@Override
		public Registration<T> cancelAfterUse() {
			this.cancelAfterUse = true;
			return this;
		}

		@Override
		public boolean isCancelAfterUse() {
			return cancelAfterUse;
		}

		@Override
		public Registration<T> cancel() {
			if (!cancelled) {
				if (lifecycle) {
					((Pausable) object).cancel();
				}
				this.cancelled = true;
			}
			return this;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public Registration<T> pause() {
			this.paused = true;
			if (lifecycle) {
				((Pausable) object).pause();
			}
			return this;
		}

		@Override
		public boolean isPaused() {
			return paused;
		}

		@Override
		public Registration<T> resume() {
			paused = false;
			if (lifecycle) {
				((Pausable) object).resume();
			}
			return this;
		}
	}

}
