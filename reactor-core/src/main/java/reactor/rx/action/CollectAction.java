package reactor.rx.action;

import com.gs.collections.impl.list.mutable.FastList;
import com.gs.collections.impl.list.mutable.MultiReaderFastList;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.event.dispatch.Dispatcher;
import reactor.event.registry.Registration;
import reactor.function.Consumer;
import reactor.function.Predicate;
import reactor.rx.StreamSubscription;
import reactor.timer.Timer;

import java.util.concurrent.TimeUnit;

/**
 * @author Jon Brisbin
 */
public class CollectAction<T> extends Action<T, Window<T>> {

	private final long                   amount;
	private final Registration           sweepTaskRegistration;
	private final MultiReaderFastList<T> values;
	private final Predicate<T>           whilePredicate;
	private final Predicate<T>           untilPredicate;
	private final Predicate<T>           distinctPredicate;


	public CollectAction(Dispatcher dispatcher,
	                     long amount,
	                     long time,
	                     TimeUnit unit,
	                     Timer timer,
	                     Predicate<T> whilePredicate,
	                     Predicate<T> untilPredicate,
	                     Predicate<T> distinctPredicate) {
		super(dispatcher, amount);
		this.amount = amount;
		this.whilePredicate = whilePredicate;
		this.untilPredicate = untilPredicate;
		this.distinctPredicate = distinctPredicate;

		if (time > 0) {
			Consumer<Long> sweepTask = new Consumer<Long>() {
				@Override
				public void accept(Long now) {
					sweepValues();
				}
			};
			this.sweepTaskRegistration = timer.schedule(sweepTask, time, unit, TimeUnit.MILLISECONDS.convert(time, unit));
		} else {
			this.sweepTaskRegistration = null;
		}

		this.values = MultiReaderFastList.newList();
	}

	@Override
	public Action<T, Window<T>> pause() {
		if (null != sweepTaskRegistration) {
			sweepTaskRegistration.pause();
		}
		return super.pause();
	}

	@Override
	public Action<T, Window<T>> resume() {
		if (null != sweepTaskRegistration) {
			sweepTaskRegistration.resume();
		}
		return super.resume();
	}

	@Override
	public Action<T, Window<T>> cancel() {
		if (null != sweepTaskRegistration) {
			sweepTaskRegistration.cancel();
		}
		return super.cancel();
	}

	@Override
	public void onComplete() {
		if (null != sweepTaskRegistration) {
			sweepTaskRegistration.cancel();
		}
		sweepValues();
		super.onComplete();
	}

	@Override
	protected void doNext(T ev) {
		if ((amount > 0 && (values.size() + 1) >= amount)) {
			values.add(ev);
			sweepValues();
			return;
		}

		if ((null != whilePredicate && !whilePredicate.test(ev))
				|| (null != untilPredicate && untilPredicate.test(ev))
				|| (null != distinctPredicate && distinctPredicate.test(ev))) {
			sweepValues();
		}
		values.add(ev);
	}

	private void sweepValues() {
		FastList<T> l = FastList.newList(values.size());
		for (int i = 0; i < amount; i++) {
			l.add(values.remove(i));
		}
		broadcastNext(new Window<T>(dispatcher, l));
	}

}
