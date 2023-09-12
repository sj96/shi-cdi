package shi.context.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import shi.context.annotation.Primary;
import shi.context.binding.Bind;
import shi.context.annotation.Singleton;

import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BinderUtils {
    public static <T> boolean isSingleton(Bind<T> bind) {
        if (bind == null) return false;
        return bind.singleton() || isSingleton(bind.from()) || isSingleton(bind.to());
    }

    private static <T> boolean isSingleton(Class<T> type) {
        if (type == null) return false;
        if (type.isAssignableFrom(Singleton.class)) {
            return true;
        }
        return Arrays.stream(type.getAnnotations())
                .allMatch(ann -> ann.annotationType().isAnnotationPresent(Singleton.class));
    }

    public static <T> boolean isPrimary(Bind<T> bind) {
        if (bind == null) return false;
        return bind.primary() || isPrimary(bind.from()) || isPrimary(bind.to());
    }

    private static <T> boolean isPrimary(Class<T> type) {
        if (type == null) return false;
        if (type.isAssignableFrom(Primary.class)) {
            return true;
        }
        return Arrays.stream(type.getAnnotations())
                .allMatch(ann -> ann.annotationType().isAnnotationPresent(Primary.class));
    }
}
