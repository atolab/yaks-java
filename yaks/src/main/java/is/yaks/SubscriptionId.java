package is.yaks;

import io.zenoh.Subscriber;

public final class SubscriptionId {

    private Subscriber sub;

    protected SubscriptionId(Subscriber sub) {
        this.sub = sub;
    }

    protected Subscriber getZSubscriber() {
        return sub;
    }
}
