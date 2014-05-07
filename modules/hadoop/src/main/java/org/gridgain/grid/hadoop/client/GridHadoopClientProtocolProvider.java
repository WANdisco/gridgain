/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.hadoop.client;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.protocol.*;
import org.gridgain.client.*;
import org.gridgain.client.marshaller.optimized.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static org.gridgain.grid.hadoop.client.GridHadoopClientProtocol.*;

/**
 * Grid Hadoop client protocol provider.
 */
public class GridHadoopClientProtocolProvider extends ClientProtocolProvider {
    /** Just heuristic to allow biasing on striped monitors. */
    private static final int CONCURRENCY_LVL = 64;

    /** Client protocol map. */
    private static final Map<Integer, ClientMap> cliMap = new HashMap<>(CONCURRENCY_LVL, 1.0f);

    static {
        // Fill protocol map with values on class load.
        for (int i = 0; i < CONCURRENCY_LVL; i++)
            cliMap.put(i, new ClientMap());
    }

    /** {@inheritDoc} */
    @Override public ClientProtocol create(Configuration conf) throws IOException {
        if (GridHadoopClientProtocol.PROP_FRAMEWORK_NAME.equals(conf.get(MRConfig.FRAMEWORK_NAME))) {
            String addr = conf.get(PROP_SRV_ADDR);

            return create0(addr, conf);
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override public ClientProtocol create(InetSocketAddress addr, Configuration conf) throws IOException {
        if (GridHadoopClientProtocol.PROP_FRAMEWORK_NAME.equals(conf.get(MRConfig.FRAMEWORK_NAME)))
            return create0(addr.getHostString() + ":" + addr.getPort(), conf);

        return null;
    }

    /** {@inheritDoc} */
    @Override public void close(ClientProtocol cliProto) throws IOException {
        assert cliProto instanceof GridHadoopClientProtocol;

        ((GridHadoopClientProtocol)cliProto).close();
    }

    /**
     * Acquire client protocol.
     *
     * @param addr Address.
     * @return Client protocol.
     * @throws IOException If failed.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    static GridClient acquire(String addr) throws IOException {
        ClientMap map = clientMap(addr);

        synchronized (map) {
            Client cli = map.get(addr);

            if (cli == null) {
                cli = new Client(createClient(addr));

                map.put(addr, cli);
            }
            else
                cli.acquire();

            return cli.client();
        }
    }

    /**
     * Release client protocol.
     *
     * @param addr Address.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    static void release(String addr) {
        ClientMap map = clientMap(addr);

        synchronized (map) {
            Client cli = map.get(addr);

            assert cli != null;

            if (cli.release())
                map.remove(addr);
        }
    }

    /**
     * Get client map for the given address.
     *
     * @param addr Address.
     * @return Client map.
     */
    private static ClientMap clientMap(String addr) {
        assert addr != null;

        int idx = U.safeAbs(addr.hashCode() % CONCURRENCY_LVL);

        ClientMap map = cliMap.get(idx);

        assert map != null;

        return map;
    }

    /**
     * Internal protocol creation routine.
     *
     * @param addr Address.
     * @param conf Configuration.
     * @return Client protocol.
     * @throws IOException If failed.
     */
    private static ClientProtocol create0(String addr, Configuration conf) throws IOException {
        return new GridHadoopClientProtocol(conf, addr, acquire(addr));
    }

    /**
     * Create client.
     *
     * @param addr Endpoint address.
     * @return Client.
     * @throws IOException If failed.
     */
    private static GridClient createClient(String addr) throws IOException {
        GridClientConfiguration cliCfg = new GridClientConfiguration();

        cliCfg.setProtocol(GridClientProtocol.TCP);
        cliCfg.setServers(Collections.singletonList(addr));
        cliCfg.setMarshaller(new GridClientOptimizedMarshaller());

        try {
            return GridClientFactory.start(cliCfg);
        }
        catch (GridClientException e) {
            throw new IOException("Failed to establish connection with GridGain node: " + addr, e);
        }
    }

    /**
     * Client map.
     */
    private static class ClientMap {
        /** Map. */
        private final Map<String, Client> map = new HashMap<>();

        /**
         * Get client.
         *
         * @param addr Address.
         * @return Client.
         */
        @Nullable private Client get(String addr) {
            assert addr != null;

            Client cli = map.get(addr);

            if (cli == null)
                assert !map.containsKey(addr);

            return cli;
        }

        /**
         * Put client.
         *
         * @param addr Address.
         * @param cli Client.
         */
        private void put(String addr, Client cli) {
            assert addr != null;
            assert cli != null;
            assert !map.containsKey(addr);

            map.put(addr, cli);
        }

        /**
         * Remove client.
         *
         * @param addr Address.
         */
        private void remove(String addr) {
            assert addr != null;
            assert map.containsKey(addr);

            map.remove(addr);
        }
    }

    /**
     * Client wrapper.
     */
    private static class Client {
        /** Client instance. */
        private final GridClient cli;

        /** Usage count. Initially set to 1. */
        private int cnt = 1;

        /**
         * Constructor.
         *
         * @param cli Client.
         */
        private Client(GridClient cli) {
            this.cli = cli;
        }

        /**
         * Acquire protocol usage.
         */
        private void acquire() {
            assert cnt > 0;

            cnt++;
        }

        /**
         * Release protocol usage.
         *
         * @return {@code True} if GG client was closed by this call.
         */
        private boolean release() {
            assert cnt > 0;

            if (--cnt == 0) {
                cli.close();

                return true;
            }
            else
                return false;
        }

        /**
         * Get GG client.
         *
         * @return GG client.
         */
        private GridClient client() {
            return cli;
        }
    }
}
