package shi.vertx.container.injectors;

import java.lang.reflect.Field;

public interface InjectorAdapter {

    boolean isInjectable(Field field);

    void inject(Object instance, Field field);
}
