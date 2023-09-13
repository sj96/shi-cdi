package shi.context;

import io.vertx.core.Future;
import org.junit.Test;
import shi.context.annotation.Inject;
import shi.context.binding.Bind;
import shi.context.factory.ComponentFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ApplicationContextTest {

    @Test
    public void test_create() {
        var context = ApplicationContext.create();
        context.registry(Bind.bind(B.class).to(B.class));
        context.registry(Bind.bind(A.class).to(A.class));
        context.registry(Bind.bind(A2.class).to(A2.class));
//        context.registry(Bind.bind(B2.class).to(B2_2.class));
        context.registry(Bind.bind(B2.class).name("b2"), new ComponentFactory<B2>() {
            @Override
            public Future<B2> create() {
                return context.vertx().executeBlocking(() -> {
                    TimeUnit.SECONDS.sleep(15);
                    return new B2_2();
                }, false);
            }
        });
        var a = context.getComponent(A.class)
                .toCompletionStage()
                .toCompletableFuture()
                .join();
    }

    static class A {
        private final List<B> bList;
        @Inject
        private A2 a2;
        private B2 b2;

        @Inject
        public A(List<B> bList) {
            this.bList = bList;
        }

        @Inject
        public void b2(B2 b2) {
            this.b2 = b2;
        }
    }

    static class A2 {

//        private final A a;
//        @Inject
//        public A2(A a) {
//            this.a = a;
//        }
    }


    static class B {

    }

    interface B2 {

    }

    static class B2_2 implements B2 {

    }
}