package shi.container.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import shi.container.annotation.Named;
import shi.container.annotation.Qualifier;

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
        var name = "";
        if (field.isAnnotationPresent(Named.class)) {
            name = field.getAnnotation(Named.class).value();
        } else {
            for (var a : field.getAnnotations()) {
                if (a.annotationType().isAnnotationPresent(Qualifier.class)) {
                    name = a.annotationType().getSimpleName();
                    break;
                }
            }
        }
        return name;
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

    public Set<Method> getDeclaredMethodsWithAnnotation(Class<?> clazz, Class<? extends Annotation> ann) {
        return findDeclaredMethods(clazz, m -> AnnotationUtils.hasAnnotation(m, ann));
    }

    public Set<Method> getMethodsWithAnnotation(Class<?> clazz, Class<? extends Annotation> ann) {
        return findMethods(clazz, m -> AnnotationUtils.hasAnnotation(m, ann));
    }

    public static List<Field> getDeclaredFieldsWithAnnotation(Class<?> clazz, Class<? extends Annotation> ann) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(m -> m.isAnnotationPresent(ann))
                .collect(Collectors.toList());
    }

    public static void makeAccessible(AccessibleObject accessibleObject) {
        accessibleObject.setAccessible(true); // NOSONAR
    }

    @SneakyThrows
    public static void setField(Field field, Object instance, Object value) {
        makeAccessible(field);
        field.set(instance, value); // NOSONAR
    }

    @SneakyThrows
    public static void invoke(Method method, Object instance, Object... args) {
        makeAccessible(method);
        method.invoke(instance, args); // NOSONAR
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

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Constructor<?> candidate, Object[] parameters) {
        ReflectionUtils.makeAccessible(candidate);
        return (T) candidate.newInstance(parameters);
    }
}
