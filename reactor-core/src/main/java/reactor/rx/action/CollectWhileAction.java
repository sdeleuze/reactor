package reactor.rx.action;

import reactor.event.dispatch.Dispatcher;
import reactor.function.Predicate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jon Brisbin
 */
public class CollectWhileAction<T> extends SequenceAction<T> {

	public CollectWhileAction(Dispatcher dispatcher, Predicate<T> whileTrueTrigger) {
		super(dispatcher, new Predicate<T>() {
			@Override
			public boolean test(T t) {
				return !whileTrueTrigger.test(t);
			}
		});
	}

	@Override
	protected void doSweep(T val, List<T> values) {
		broadcastNext(new Window<T>(dispatcher, new ArrayList<T>(values)));
		values.clear();
		values.add(val);
	}

}
