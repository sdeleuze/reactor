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
	protected void doComplete() {
		super.doComplete();
		doSweep();
	}

	@Override
	protected void doSweep(T val, List<T> values) {
		values.add(val);
		broadcastNext(new Window<T>(dispatcher, new ArrayList<T>(values)));
		onSequenceEnd(values);
		values.clear();
	}

}
