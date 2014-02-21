package reactor.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public class TraceableDelegatingFilter implements Filter {

	private final Filter delegate;
	private final Logger log;

	public TraceableDelegatingFilter(Filter delegate) {
		Assert.notNull(delegate, "Delegate Filter cannot be null.");
		this.delegate = delegate;
		this.log = LoggerFactory.getLogger(delegate.getClass());
	}

	@Override
	public <T> Iterable<T> filter(Iterable<T> items, Object key) {
		if(log.isTraceEnabled()) {
			log.trace("filtering {} using key {}", items, key);
		}
		Iterable<T> l = delegate.filter(items, key);
		if(log.isTraceEnabled()) {
			log.trace("items {} matched key {}", (null == items ? Collections.emptyList() : items), key);
		}
		return l;
	}

}
