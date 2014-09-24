package reactor.rx.action;

import reactor.event.dispatch.Dispatcher;
import reactor.function.Function;
import reactor.function.Predicate;

/**
 * @author Jon Brisbin
 */
public class CollectDistinctByKeyAction<T, K> extends CollectWhileAction<T> {

	public CollectDistinctByKeyAction(Dispatcher dispatcher, Function<T, K> keyMapper) {
		super(dispatcher, new DistinctByKeyPredicate<T, K>(keyMapper));
	}

	private static class DistinctByKeyPredicate<T, K> implements Predicate<T> {
		private final Function<T, K> keyMapper;

		private K last;

		private DistinctByKeyPredicate(Function<T, K> keyMapper) {
			this.keyMapper = keyMapper;
		}

		@Override
		public boolean test(T t) {
			K key = keyMapper.apply(t);
			boolean distinct = (last != key || !last.equals(key));
			last = key;
			return distinct;
		}
	}

}
