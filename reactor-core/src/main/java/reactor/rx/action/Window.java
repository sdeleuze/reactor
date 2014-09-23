package reactor.rx.action;

import reactor.event.dispatch.Dispatcher;

import java.util.Iterator;
import java.util.List;

/**
 * @author Jon Brisbin
 */
public class Window<T> extends ForEachAction<T> implements Iterable<T> {

	private final List<T> values;

	public Window(Dispatcher dispatcher, List<T> values) {
		super(values, dispatcher);
		this.values = values;
	}

	@Override
	public Iterator<T> iterator() {
		return values.iterator();
	}

}
