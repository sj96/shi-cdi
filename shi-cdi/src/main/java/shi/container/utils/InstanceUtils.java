package shi.container.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstanceUtils {

    @SuppressWarnings("unchecked")
    public static <T> T createInstance(Class<T> type) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        var constructor = Arrays.stream(type.getConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .filter(c -> c.getParameterCount() == 0)
                .findFirst();
        if (constructor.isPresent()) {
            return (T) constructor.get().newInstance();
        }
        throw new InstantiationException("Not found default construct of " + type);
    }
}
