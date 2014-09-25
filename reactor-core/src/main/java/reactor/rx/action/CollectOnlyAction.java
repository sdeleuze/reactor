package reactor.rx.action;

import reactor.event.dispatch.Dispatcher;
import reactor.function.Predicate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jon Brisbin
 */
public class CollectOnlyAction<T> extends SequenceAction<T> {

	private final long amount;

	private       boolean      atStart      = true;
	private       boolean      atEnd        = false;
	private final Predicate<T> countTrigger = new Predicate<T>() {
		private long count = 0;

		@Override
		public boolean test(T t) {
			if (++count == amount) {
				count = 0;
				atStart = true;
				atEnd = false;
				return true;
			} else {
				return false;
			}
		}
	};

	public CollectOnlyAction(Dispatcher dispatcher, long amount) {
		super(dispatcher, null);
		this.amount = amount;
	}

	@Override
	protected boolean isStart() {
		return atStart;
	}

	@Override
	protected boolean isEnd() {
		return atEnd;
	}

	@Override
	protected Predicate<T> getSweepTrigger() {
		return countTrigger;
	}

	@Override
	protected void doSweep(T val, List<T> values) {
		values.add(val);
		broadcastNext(new ArrayList<T>(values));
		atEnd = true;
	}

	@Override
	protected void onSequenceStart(List<T> values) {
		super.onSequenceStart(values);
		atStart = false;
	}

	@Override
	protected void onSequenceEnd(List<T> values) {
		super.onSequenceEnd(values);
		atStart = true;
		atEnd = false;
	}

}
