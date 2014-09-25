package reactor.rx.action;

import reactor.event.dispatch.Dispatcher;
import reactor.function.Predicate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jon Brisbin
 */
public class CollectDistinctAction<T> extends SequenceAction<T> implements Predicate<T> {

	private T last;

	public CollectDistinctAction(Dispatcher dispatcher) {
		super(dispatcher, null);
	}

	@Override
	protected Predicate<T> getSweepTrigger() {
		return (null == super.getSweepTrigger() ? this : super.getSweepTrigger());
	}

	@Override
	protected void doSweep(T val, List<T> values) {
		broadcastNext(new ArrayList<T>(values));
		onSequenceEnd(values);
		if (null != val) {
			values.add(val);
		}
	}

	@Override
	public boolean test(T t) {
		boolean distinct = (null != last && (last != t || !last.equals(t)));
		last = t;
		return distinct;
	}

}
