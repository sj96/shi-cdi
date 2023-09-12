package shi.vertx.container.factories;

import org.junit.Test;
import shi.vertx.container.ApplicationContext;
import shi.vertx.container.annotations.Component;
import shi.vertx.container.binding.Bind;
import shi.vertx.container.exceptions.EnvironmentException;

import javax.inject.Inject;
import java.util.List;

import static org.junit.Assert.*;

public class InstanceFactoryTest {

    @Test
    public void test1() {
        var ctx = ApplicationContext.create();
        var a = ctx.getInstance(A.class);
        assertEquals(0, a.bList.size());
    }

    @Test
    public void test1_2() {
        var ctx = ApplicationContext.create();
        ctx.registry(Bind.bind(A.class).name("a1").to(A.class));
        var a = ctx.getInstance(A.class);
        assertEquals(0, a.bList.size());
    }

    @Test
    public void test2() {
        var ctx = ApplicationContext.create();
        ctx.registry(Bind.bind(B.class).name("b1").to(B.class));
        ctx.registry(Bind.bind(B.class).name("b2").to(B.class));
        var a = ctx.getInstance(A.class);
        assertEquals(2, a.bList.size());
    }

    @Test
    public void test3() {
        try {
            var ctx = ApplicationContext.create();
            ctx.registry(Bind.bind(B.class).name("b1").to(B.class).primary(true));
            ctx.registry(Bind.bind(B.class).name("b2").to(B.class).primary(true));
        } catch (EnvironmentException e) {
            assertEquals(1006, e.getError().code());
        }
    }

    @Test
    public void test3_1() {
        try {
            var ctx = ApplicationContext.create();
            ctx.registry(Bind.bind(B.class).name("b1").to(B.class));
            ctx.registry(Bind.bind(B.class).name("b2").to(B.class));
            var a = ctx.getInstance(A2.class);
            assertNotNull(a);
        } catch (EnvironmentException e) {
            assertEquals(1005, e.getError().code());
        }
    }

    @Test
    public void test4() {
        var ctx = ApplicationContext.create();
        ctx.registry(Bind.bind(B.class).name("b1").to(B.class).primary(true));
        ctx.registry(Bind.bind(B.class).name("b2").to(B.class));
        var a = ctx.provide(A2.class);
        assertNotNull(a);
    }

    @Test
    public void test5() {
        var ctx = ApplicationContext.create();
        var b = ctx.provide(B.class);
        assertNull(b);
    }

    @Test
    public void test6() {
        try {
            var ctx = ApplicationContext.create();
            ctx.getInstance(B2.class);
        } catch (EnvironmentException e) {
            assertEquals(1000, e.getError().code());
        }
    }

    @Component
    static class A {
        private final List<B> bList;

        @Inject
        public A(List<B> bList) {
            this.bList = bList;
        }
    }

    static class A2 {

        @Inject
        public A2(B b) {
        }
    }


    static class B {

    }

    interface B2 {

    }
}