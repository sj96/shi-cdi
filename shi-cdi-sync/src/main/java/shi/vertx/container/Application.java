/*
 * Copyright 2016 JSpare.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package shi.vertx.container;

import lombok.SneakyThrows;
import shi.vertx.container.utils.InstanceUtils;

public interface Application {

    @SneakyThrows
    static Application create(Class<? extends Application> bootstrapClazz) {
        return InstanceUtils.createInstance(bootstrapClazz);
    }

    @SneakyThrows
    static void run(Class<? extends Application> bootstrapClazz) {
        create(bootstrapClazz).run();
    }

    @SneakyThrows
    default void run() {
        Environment.create();
        setup();
        inject();
        start();
    }

    void start() throws Exception;

    default void setup() {
    }

    default void inject() {
        Environment.inject(this);
    }

}
