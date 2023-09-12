package shi.context;

import org.junit.Test;
import shi.context.annotation.Inject;
import shi.context.binding.Bind;

import java.util.List;

public class ApplicationContextTest {

    @Test
    public void test_create() {
        var context = ApplicationContext.create();
        context.registry(Bind.bind(B.class).to(B.class));
        context.registry(Bind.bind(A.class).to(A.class));
        context.registry(Bind.bind(A2.class).to(A2.class));
        context.registry(Bind.bind(B2.class).to(B2.class));
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
}