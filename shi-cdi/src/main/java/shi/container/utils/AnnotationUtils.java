package shi.container.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnnotationUtils {

    public static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotation) {
        if (element.isAnnotationPresent(annotation)) return true;
        if (element instanceof Method) {
            return methodHasAnnotation((Method) element, annotation);
        }
        if (element instanceof Class<?>) {
            return classHasAnnotation((Class<?>) element, annotation);
        }
        return false;
    }

    private static boolean classHasAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        if (clazz.getSuperclass() != null) {
            return hasAnnotation(clazz.getSuperclass(), annotation);
        }
        for (var inf : clazz.getInterfaces()) {
            if (inf.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

    private static boolean methodHasAnnotation(Method method, Class<? extends Annotation> annotation) {
        var declaringClass = method.getDeclaringClass();
        if (declaringClass.getSuperclass() != null) {
            try {
                var superMethod = declaringClass.getSuperclass().getDeclaredMethod(method.getName(), method.getParameterTypes());
                return hasAnnotation(superMethod, annotation);
            } catch (NoSuchMethodException e) {
                // ignored
            }
        }
        for (var inf : declaringClass.getInterfaces()) {
            try {
                var interfaceMethod = inf.getDeclaredMethod(method.getName(), method.getParameterTypes());
                if (interfaceMethod.isAnnotationPresent(annotation)) {
                    return true;
                }
            } catch (NoSuchMethodException e) {
                // ignored
            }
        }
        return false;
    }
}
