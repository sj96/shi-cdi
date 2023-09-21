package shi.container.injectors;

import io.vertx.core.Future;

public interface MembersInjector {

    Future<Void> inject(Object instance);
}
