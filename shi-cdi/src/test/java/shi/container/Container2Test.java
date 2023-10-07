package shi.container;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.RequiredArgsConstructor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import shi.container.bind.Bind;

import java.util.concurrent.TimeUnit;

@RunWith(VertxUnitRunner.class)
public class Container2Test {
    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Test
    public void test_create(TestContext testContext) {
        var context = Container.create(rule.vertx());
//        context.registry(Bind.bind(InjectVerticle.class).to(InjectVerticle.class));
        context.registry(Bind.bind(Component1.class).to(Component1.class));

        var async = testContext.async();
        rule.vertx().deployVerticle("inject:" + InjectVerticle.class.getName()).onComplete(ar -> {
            testContext.assertTrue(ar.succeeded());
            async.complete();
        });
//        context.createComponent(InjectVerticle.class)
//                .compose(verticle -> rule.vertx().deployVerticle(verticle, new DeploymentOptions().setHa(true)))
//                .onComplete(ar -> {
//                });
    }

    @RequiredArgsConstructor
    static class InjectVerticle extends AbstractVerticle {
        private final Component1 component1;
    }

    static class Component1 {
        public Component1() throws InterruptedException {
            TimeUnit.SECONDS.sleep(5); // NOSONAR
        }
    }
}