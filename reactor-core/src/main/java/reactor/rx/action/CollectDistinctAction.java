package reactor.rx.action;

import reactor.event.dispatch.Dispatcher;
import reactor.function.Predicate;

/**
 * @author Jon Brisbin
 */
public class CollectDistinctAction<T> extends CollectWhileAction<T> {

	public CollectDistinctAction(Dispatcher dispatcher) {
		this(dispatcher, new DistinctPredicate<T>());
	}

	public CollectDistinctAction(Dispatcher dispatcher, Predicate<T> distinctTrigger) {
		super(dispatcher, distinctTrigger);
	}

	private static class DistinctPredicate<T> implements Predicate<T> {
		private T last;

		@Override
		public boolean test(T t) {
			boolean distinct = (last != t || !last.equals(t));
			last = t;
			return distinct;
		}
	}

}
