package reactor.rx.action;

import reactor.event.dispatch.Dispatcher;
import reactor.function.Predicate;

/**
 * @author Jon Brisbin
 */
public class CollectWhileAction<T> extends CollectUntilAction<T> {

	public CollectWhileAction(Dispatcher dispatcher, Predicate<T> whileTrueTrigger) {
		super(dispatcher, whileTrueTrigger);
	}

	@Override
	protected boolean shouldSweep(T val) {
		return !super.shouldSweep(val);
	}

}
