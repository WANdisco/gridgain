/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.datastructures.partitioned;

import org.gridgain.grid.cache.*;

import static org.gridgain.grid.cache.GridCacheAtomicWriteOrderMode.*;
import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;

/**
 *
 */
public class GridCachePartitionedAtomicQueueCreateMultiNodeSelfTest
    extends GridCachePartitionedQueueCreateMultiNodeSelfTest {
    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration cacheConfiguration() {
        GridCacheConfiguration ccfg = super.cacheConfiguration();

        ccfg.setAtomicityMode(ATOMIC);
        ccfg.setAtomicWriteOrderMode(PRIMARY);

        return ccfg;
    }
}