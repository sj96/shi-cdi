package shi.context.factory;

import io.vertx.core.Future;

public interface ComponentFactory<T> {
    Future<T> create();
}
