package shi.vertx.container.binding;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import shi.vertx.container.Environment;

@Data
@Accessors(fluent = true)
public class Bind<T> {

    private Class<T> from;
    private String name = StringUtils.EMPTY;
    private Class<?> to;
    private boolean singleton;
    private boolean primary;

    public static <T> Bind<T> bind(Class<T> clazz) {
        return new Bind<T>().from(clazz);
    }

    public Key bindKey() {
        return new Key(from, name);
    }

    public void registry() {
        Environment.registry(this);
    }

    public void registry(Object instance) {
        Environment.registry(this, instance);
    }

}
