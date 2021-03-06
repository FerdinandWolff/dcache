/*
 * dCache - http://www.dcache.org/
 *
 * Copyright (C) 2016 Deutsches Elektronen-Synchrotron
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package diskCacheV111.doors;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import diskCacheV111.util.CheckStagePermission;

import dmg.cells.nucleus.CellAddressCore;
import dmg.cells.nucleus.CellEndpoint;
import dmg.cells.nucleus.CellPath;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.dcache.auth.CachingLoginStrategy;
import org.dcache.auth.LoginStrategy;
import org.dcache.auth.UnionLoginStrategy;
import org.dcache.auth.attributes.Restriction;
import org.dcache.auth.attributes.Restrictions;
import org.dcache.cells.CellStub;
import org.dcache.poolmanager.PoolManagerHandler;
import org.dcache.poolmanager.PoolManagerStub;
import org.dcache.services.login.RemoteLoginStrategy;
import org.dcache.util.Option;

import static diskCacheV111.doors.DCapDoorInterpreterV3._log;

public class DcapDoorSettings
{
    @Option(name = "authorization")
    protected String auth;

    @Option(name = "anonymous-access")
    protected String anon;

    @Option(name = "poolManager",
            description = "Cell address of the pool manager",
            defaultValue = "PoolManager")
    protected CellPath poolManager;

    @Option(name = "pnfsManager",
            description = "Cell address of the PNFS manager",
            defaultValue = "PnfsManager")
    protected CellPath pnfsManager;

    @Option(name = "pinManager",
            description = "Cell address of the pin manager",
            defaultValue = "PinManager")
    protected CellPath pinManager;

    @Option(name = "gplazma",
            description = "Cell address of GPlazma",
            defaultValue = "gplazma")
    protected CellPath gPlazma;

    @Option(name = "billing",
            description = "Cell address of billing",
            defaultValue = "billing")
    protected CellPath billing;

    @Option(name = "kafka",
            description = "Kafka service enabled",
            defaultValue = "false")
    protected boolean isKafkaEnabled;

    @Option(name = "bootstrap-server")
    protected String bootstrapServer;

    @Option(name = "max-block-ms")
    protected String maxBlockMs;

    @Option(name = "retries")
    protected String retries;


    @Option(name = "hsm",
            description = "Cell address of hsm manager",
            defaultValue = "hsm")
    protected CellPath hsmManager;

    @Option(name = "truncate")
    protected boolean isTruncateAllowed;

    @Option(name = "allow-access-policy-overwrite")
    protected boolean isAccessLatencyOverwriteAllowed;

    @Option(name = "allow-retention-policy-overwrite")
    protected boolean isRetentionPolicyOverwriteAllowed;

    @Option(name = "check", defaultValue = "true")
    protected boolean isCheckStrict;

    // A string with the following format:
    // majorMin.minorMin[.bugfixMin[-packageMin]][:majorMax.minorMax[.bugfix[-packageMax]]
    @Option(name = "clientVersion")
    protected String clientVersion;

    @Option(name = "poolRetry", defaultValue = "0")
    protected long poolRetry;

    @Option(name = "io-queue")
    protected String ioQueueName;

    @Option(name = "io-queue-overwrite")
    protected boolean ioQueueAllowOverwrite;

    @Option(name = "read-only")
    protected boolean isReadOnly;

    @Option(name = "stageConfigurationFilePath")
    protected String stageConfigurationFilePath;

    /**
     * If true, then the Subject of the request must have a UID and
     * GID. If false, then a Subject without a UID and GID (i.e. a
     * Nobody) will be allowed to proceed, but only allowed to perform
     * operations authorized to world.
     */
    private boolean isAuthorizationStrong;

    /**
     * If false, then authorization checks on read and write
     * operations are bypassed for non URL operations. If true, then
     * such operations are subject to authorization checks.
     */
    private boolean isAuthorizationRequired;

    private UnionLoginStrategy.AccessLevel anonymousAccessLevel;

    private DCapDoorInterpreterV3.Version minClientVersion = new DCapDoorInterpreterV3.Version(0, 0);

    private DCapDoorInterpreterV3.Version maxClientVersion = new DCapDoorInterpreterV3.Version(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private Restriction doorRestriction;

    private CheckStagePermission checkStagePermission;

    public void init()
    {
        isAuthorizationStrong = (auth != null) && auth.equals("strong");
        isAuthorizationRequired = (auth != null) && (auth.equals("strong") || auth.equals("required"));
        anonymousAccessLevel = (anon != null)
                               ? UnionLoginStrategy.AccessLevel.valueOf(anon.toUpperCase())
                               : UnionLoginStrategy.AccessLevel.READONLY;

        if (clientVersion != null) {
            try {
                List<String> values = Splitter.on(':').limit(2).trimResults().splitToList(clientVersion);
                if (values.get(0).isEmpty()) {
                    throw new IllegalArgumentException("missing minimum version");
                }
                minClientVersion  = new DCapDoorInterpreterV3.Version(values.get(0));
                if (values.size() > 1) {
                    if (values.get(1).isEmpty()) {
                        throw new IllegalArgumentException("missing maximum version");
                    }
                    maxClientVersion  = new DCapDoorInterpreterV3.Version(values.get(1));
                }
            } catch (IllegalArgumentException e) {
                _log.error("Ignoring client version limits: syntax error with '{}': {}", clientVersion, e.getMessage());
            }
        }

        ioQueueName = Strings.emptyToNull(ioQueueName);

        doorRestriction = isReadOnly ? Restrictions.readOnly() : Restrictions.none();

        checkStagePermission = new CheckStagePermission(stageConfigurationFilePath);
    }

    public boolean isAuthorizationRequired()
    {
        return isAuthorizationRequired;
    }

    public CellPath getPnfsManager()
    {
        return pnfsManager;
    }

    public CellPath getPoolManager()
    {
        return poolManager;
    }

    public CellPath getPinManager()
    {
        return pinManager;
    }

    public CellPath getGplazma()
    {
        return gPlazma;
    }

    public CellPath getBilling()
    {
        return billing;
    }

    public boolean isKafkaEnabled() { return isKafkaEnabled; }

    public String getBootstrapServer() { return bootstrapServer; }

    public String getMaxBlockMs() { return maxBlockMs; }

    public String getRetries() { return retries; }

    public CellPath getHsmManager()
    {
        return hsmManager;
    }

    public boolean isTruncateAllowed()
    {
        return isTruncateAllowed;
    }

    public boolean isAccessLatencyOverwriteAllowed()
    {
        return isAccessLatencyOverwriteAllowed;
    }

    public boolean isRetentionPolicyOverwriteAllowed()
    {
        return isRetentionPolicyOverwriteAllowed;
    }

    public boolean isCheckStrict()
    {
        return isCheckStrict;
    }

    public DCapDoorInterpreterV3.Version getMinClientVersion()
    {
        return minClientVersion;
    }

    public DCapDoorInterpreterV3.Version getMaxClientVersion()
    {
        return maxClientVersion;
    }

    public long getPoolRetry()
    {
        return poolRetry * 1000;
    }

    public String getIoQueueName()
    {
        return ioQueueName;
    }

    public boolean isIoQueueAllowOverwrite()
    {
        return ioQueueAllowOverwrite;
    }

    public Restriction getDoorRestriction()
    {
        return doorRestriction;
    }

    public CheckStagePermission getCheckStagePermission()
    {
        return checkStagePermission;
    }

    public LoginStrategy createLoginStrategy(CellEndpoint cell)
    {
        UnionLoginStrategy union = new UnionLoginStrategy();
        if (isAuthorizationStrong || isAuthorizationRequired) {
            RemoteLoginStrategy loginStrategy = new RemoteLoginStrategy(new CellStub(cell, gPlazma, 30000));
            union.setLoginStrategies(Collections.singletonList(loginStrategy));
        }
        if (!isAuthorizationStrong) {
            union.setAnonymousAccess(anonymousAccessLevel);
        }
        return new CachingLoginStrategy(union, 1, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public PoolManagerStub createPoolManagerStub(CellEndpoint cellEndpoint, CellAddressCore address, PoolManagerHandler handler)
    {
        PoolManagerStub stub = new PoolManagerStub();
        stub.setCellEndpoint(cellEndpoint);
        stub.setCellAddress(address);
        stub.setHandler(handler);
        stub.setMaximumPoolManagerTimeout(20000);
        stub.setMaximumPoolManagerTimeoutUnit(TimeUnit.MILLISECONDS);
        return stub;
    }

    public KafkaProducer createKafkaProducer(String bootstrap_server,
                                             String client_id,
                                             String max_block_ms,
                                             String retries)
    {
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_server);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, client_id);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.dcache.notification.DoorRequestMessageSerializer");
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, max_block_ms);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);

        return new KafkaProducer<>(props);
    }


    public CellStub createPinManagerStub(CellEndpoint cell)
    {
        return new CellStub(cell, pinManager);
    }
}
