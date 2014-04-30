/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.examples;

import org.gridgain.client.router.*;
import org.gridgain.client.router.impl.*;
import org.gridgain.examples.misc.client.router.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;
import org.springframework.beans.factory.*;
import org.springframework.context.*;

import java.net.*;
import java.util.*;

/**
 * GridRouterExample self test.
 */
public class GridRouterExamplesSelfTest extends GridAbstractExamplesTest {
    /**
     * @throws Exception If failed.
     */
    @Override protected void beforeTest() throws Exception {
        // Start up a grid node.
        startGrid("grid-router-examples", "examples/config/example-cache.xml");
        // Start up a router.
        startRouter("modules/clients/src/main/java/config/router/default-router.xml");
    }

    /**
     * @throws Exception If failed.
     */
    @Override protected void afterTest() throws Exception {
        GridRouterFactory.stopAllRouters();

        super.afterTest();
    }

    /**
     * @throws Exception If failed.
     */
    public void testGridRouterExample() throws Exception {
        RouterExample.main(EMPTY_ARGS);
    }

    /**
     * Starts router.
     *
     * @param cfgPath Path to router config.
     * @throws GridException Thrown in case of any errors.
     */
    protected static void startRouter(String cfgPath) throws GridException {
        URL cfgUrl = U.resolveGridGainUrl(cfgPath);

        if (cfgUrl == null)
            throw new GridException("Spring XML file not found (is GRIDGAIN_HOME set?): " + cfgPath);

        ApplicationContext ctx = GridRouterCommandLineStartup.loadCfg(cfgUrl);

        if (ctx == null)
            throw new GridException("Application context can not be null");

        GridTcpRouterConfiguration tcpCfg = getBean(ctx, GridTcpRouterConfiguration.class);

        if (tcpCfg == null)
            throw new GridException("GridTcpRouterConfiguration is not found");

        GridRouterFactory.startTcpRouter(tcpCfg);

        GridHttpRouterConfiguration httpCfg = getBean(ctx, GridHttpRouterConfiguration.class);

        if (httpCfg == null)
            throw new GridException("GridHttpRouterConfiguration is not found");

        httpCfg.setJettyConfigurationPath("modules/clients/src/main/java/config/router/router-jetty.xml");

        GridRouterFactory.startHttpRouter(httpCfg);
    }

    /**
     * Get bean configuration.
     *
     * @param ctx Spring context.
     * @param beanCls Bean class.
     * @return Spring bean.
     */
    @Nullable public static <T> T getBean(ListableBeanFactory ctx, Class<T> beanCls) {
        Map.Entry<String, T> entry = F.firstEntry(ctx.getBeansOfType(beanCls));

        return entry == null ? null : entry.getValue();
    }
}
