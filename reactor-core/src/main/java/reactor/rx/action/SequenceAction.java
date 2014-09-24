package reactor.rx.action;

import reactor.event.dispatch.Dispatcher;
import reactor.function.Predicate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jon Brisbin
 */
public abstract class SequenceAction<T> extends Action<T, Window<T>> {

	private final Predicate<T> sweepTrigger;
	private final List<T>      values;

	public SequenceAction(Dispatcher dispatcher, Predicate<T> sweepTrigger) {
		super(dispatcher);
		this.sweepTrigger = sweepTrigger;
		this.values = new ArrayList<T>();
	}

	@Override
	protected void doNext(T val) {
		if (isStart()) {
			onSequenceStart(values);
		}

		if (null != getSweepTrigger() && getSweepTrigger().test(val)) {
			doSweep(val, values);
		} else {
			onSequenceNext(val, values);
		}

		if (isEnd()) {
			onSequenceEnd(values);
		}
	}

	protected Predicate<T> getSweepTrigger() {
		return sweepTrigger;
	}

	protected boolean isStart() {
		return false;
	}

	protected boolean isEnd() {
		return false;
	}

	protected void doSweep() {
		doSweep(null, values);
	}

	protected abstract void doSweep(T val, List<T> values);

	protected void onSequenceStart(List<T> values) {
	}

	protected void onSequenceNext(T val, List<T> values) {
		if (null != val) {
			values.add(val);
		}
	}

	protected void onSequenceEnd(List<T> values) {
		values.clear();
	}

}
