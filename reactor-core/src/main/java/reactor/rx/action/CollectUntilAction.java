package reactor.rx.action;

import reactor.event.dispatch.Dispatcher;
import reactor.function.Predicate;

/**
 * @author Jon Brisbin
 */
public class CollectUntilAction<T> extends CollectWhileAction<T> {

	public CollectUntilAction(Dispatcher dispatcher, Predicate<T> whenTrueTrigger) {
		super(dispatcher, whenTrueTrigger);
	}

}
