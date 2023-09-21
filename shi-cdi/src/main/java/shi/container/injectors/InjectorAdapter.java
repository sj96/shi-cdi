package shi.container.injectors;

import io.vertx.core.Future;

import java.lang.reflect.Field;

public interface InjectorAdapter {

    boolean isInjectable(Field field);

    Future<Void> inject(Object instance, Field field);
}
