package reactor.core.subscription;

import org.reactivestreams.Subscription;

/**
 * A singleton Subscription that represents a cancelled subscription instance and should not be leaked to clients as it
 * represents a terminal state. <br> If algorithms need to hand out a subscription, replace this with {@link
 * EmptySubscription#INSTANCE} because there is no standard way to tell if a Subscription is cancelled or not
 * otherwise.
 */
public enum CancelledSubscription implements Subscription {
    INSTANCE;

    @Override
    public void request(long n) {
        // deliberately no op
    }

    @Override
    public void cancel() {
        // deliberately no op
    }

}
