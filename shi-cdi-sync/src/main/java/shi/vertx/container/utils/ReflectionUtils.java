package shi.vertx.container.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@UtilityClass
public class ReflectionUtils {

    public String getQualifier(AnnotatedElement field) {
        var name = StringUtils.EMPTY;
        if (field.isAnnotationPresent(Named.class)) {
            name = field.getAnnotation(Named.class).value();
        } else {
            for (var a : field.getAnnotations()) {
                if (a.annotationType().isAnnotationPresent(Qualifier.class)) {
                    name = a.annotationType().getSimpleName();
                }
            }
        }
        return name;
    }

    public Set<Method> getPostConstructMethods(Class<?> type) {
        // With PostConstruct module
        // Non-Static
        // Without parameters
        return findDeclaredMethods(type,
                m -> !Modifier.isStatic(m.getModifiers()),
                m -> m.isAnnotationPresent(PostConstruct.class),
                m -> m.getParameterCount() == 0
        );
    }

    public Set<Class<?>> collectInterfaces(Class<?> clazz) {
        var interfaces = new HashSet<>(Arrays.asList(clazz.getInterfaces()));
        if (clazz.getSuperclass() != null) {
            interfaces.addAll(collectInterfaces(clazz.getSuperclass()));
        }
        return interfaces;
    }

    public boolean hasGenericParameterizedInterfaces(Class<?> clazz) {
        return Arrays.stream(clazz.getGenericInterfaces()).anyMatch(ParameterizedType.class::isInstance);
    }

    public Set<Method> getMethodsWithAnnotation(Class<?> clazz, Class<? extends Annotation> ann) {
        return findDeclaredMethods(clazz, m -> AnnotationUtils.hasAnnotation(m, ann));
    }

    public static List<Field> getFieldsWithAnnotation(Class<?> clazz, Class<? extends Annotation> ann) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(m -> m.isAnnotationPresent(ann))
                .collect(Collectors.toList());
    }

    public static void makeAccessible(AccessibleObject accessibleObject) {
        accessibleObject.setAccessible(true); // NOSONAR
    }

    @SneakyThrows
    public static void setField(Field field, Object instance, Object value) {
        try {
            makeAccessible(field);
            field.set(instance, value); // NOSONAR
        } catch (IllegalAccessException e) {
            // ignored
        }
    }

    @SneakyThrows
    public static void invoke(Method method, Object instance, Object... args) {
        try {
            makeAccessible(method);
            method.invoke(instance, args); // NOSONAR
        } catch (IllegalAccessException | InvocationTargetException e) {
            // ignored
        }
    }

    @SafeVarargs
    public static Set<Field> findDeclaredFields(Class<?> type, Predicate<Field>... predicates) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> {
                    if (predicates != null) {
                        for (var predicate : predicates) {
                            if (!predicate.test(field)) {
                                return false;
                            }
                        }
                    }
                    return true;
                })
                .collect(Collectors.toSet());
    }

    @SafeVarargs
    public static Set<Method> findDeclaredMethods(Class<?> type, Predicate<Method>... predicates) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(field -> {
                    if (predicates != null) {
                        for (var predicate : predicates) {
                            if (!predicate.test(field)) {
                                return false;
                            }
                        }
                    }
                    return true;
                })
                .collect(Collectors.toSet());
    }

    @SafeVarargs
    public static Set<Method> findMethods(Class<?> type, Predicate<Method>... predicates) {
        return Arrays.stream(type.getMethods())
                .filter(field -> {
                    if (predicates != null) {
                        for (var predicate : predicates) {
                            if (!predicate.test(field)) {
                                return false;
                            }
                        }
                    }
                    return true;
                })
                .collect(Collectors.toSet());
    }
}
