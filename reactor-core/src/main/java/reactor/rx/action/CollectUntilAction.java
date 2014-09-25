package reactor.rx.action;

import reactor.event.dispatch.Dispatcher;
import reactor.function.Predicate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jon Brisbin
 */
public class CollectUntilAction<T> extends SequenceAction<T> {

	public CollectUntilAction(Dispatcher dispatcher, Predicate<T> whenTrueTrigger) {
		super(dispatcher, whenTrueTrigger);
	}

	@Override
	protected void doSweep(T val, List<T> values) {
		broadcastNext(new ArrayList<T>(values));
		onSequenceEnd(values);
		if (null != val) {
			values.add(val);
		}
	}

}
