package reactor.rx.action;

import org.jetbrains.annotations.NotNull;
import reactor.event.dispatch.Dispatcher;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jon Brisbin
 */
public class Window<T> extends ForEachAction<T> implements Collection<T> {

	private final List<T> values;

	public Window(Dispatcher dispatcher, List<T> values) {
		super(values, dispatcher);
		this.values = values;
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	public boolean isEmpty() {
		return values.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return values.contains(o);
	}

	@NotNull
	@Override
	public Object[] toArray() {
		return values.toArray();
	}

	@NotNull
	@Override
	public <T1> T1[] toArray(T1[] a) {
		return values.toArray(a);
	}

	@Override
	public boolean add(T t) {
		return values.add(t);
	}

	@Override
	public boolean remove(Object o) {
		return values.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return values.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return values.addAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return values.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return values.retainAll(c);
	}

	@Override
	public void clear() {
		values.clear();
	}

	@NotNull
	@Override
	public Iterator<T> iterator() {
		return values.iterator();
	}

}
