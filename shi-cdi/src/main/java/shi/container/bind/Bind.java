package shi.container.bind;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Bind<T> {
    private final Class<T> from;
    private String name = "";
    private Class<?> to;
    private boolean singleton;
    private boolean primary;

    public static <T> Bind<T> bind(Class<T> clazz) {
        return new Bind<>(clazz);
    }

    public Bind<T> name(String name) {
        if (name == null || name.isBlank()) {
            return this;
        }
        this.name = name;
        return this;
    }

    public Key bindKey() {
        return new Key(from, name);
    }

}
