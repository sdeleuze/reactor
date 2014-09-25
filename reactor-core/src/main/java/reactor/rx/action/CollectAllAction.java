package reactor.rx.action;

import reactor.event.dispatch.Dispatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jon Brisbin
 */
public class CollectAllAction<T> extends SequenceAction<T> {

	public CollectAllAction(Dispatcher dispatcher) {
		super(dispatcher, null);
	}

	@Override
	protected void doSweep(T val, List<T> values) {
		if (null != val) {
			values.add(val);
		}
		broadcastNext(new ArrayList<T>(values));
		values.clear();
	}

}
