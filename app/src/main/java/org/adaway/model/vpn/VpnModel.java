package org.adaway.model.vpn;

import static org.adaway.model.adblocking.AdBlockMethod.VPN;
import static org.adaway.model.error.HostError.ENABLE_VPN_FAIL;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.LruCache;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.adaway.R;
import org.adaway.db.AppDatabase;
import org.adaway.db.dao.HostEntryDao;
import org.adaway.db.dao.HostListItemDao;
import org.adaway.db.entity.HostEntry;
import org.adaway.db.entity.HostListItem;
import org.adaway.db.entity.ListType;
import org.adaway.model.adblocking.AdBlockMethod;
import org.adaway.model.adblocking.AdBlockModel;
import org.adaway.model.error.HostErrorException;
import org.adaway.util.AppExecutors;
import org.adaway.util.RegexUtils;
import org.adaway.vpn.VpnService;
import org.adaway.vpn.VpnServiceControls;
import org.adaway.vpn.VpnStatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * This class is the model to represent VPN service configuration.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class VpnModel extends AdBlockModel {
    private static class CompiledWildcardRule {
        final Pattern pattern;
        final ListType type;
        final String redirection;

        CompiledWildcardRule(Pattern pattern, ListType type, String redirection) {
            this.pattern = pattern;
            this.type = type;
            this.redirection = redirection;
        }
    }

    private final HostEntryDao hostEntryDao;
    private final HostListItemDao hostListItemDao;
    private final LruCache<String, HostEntry> blockCache;
    private final LinkedHashSet<String> logs;
    private volatile List<CompiledWildcardRule> wildcardRules = Collections.emptyList();
    private boolean recordingLogs;
    private int requestCount;

    /**
     * Constructor.
     *
     * @param context The application context.
     */
    public VpnModel(Context context) {
        super(context);
        AppDatabase database = AppDatabase.getInstance(context);
        this.hostEntryDao = database.hostEntryDao();
        this.hostListItemDao = database.hostsListItemDao();
        this.blockCache = new LruCache<String, HostEntry>(4 * 1024) {
            @Override
            protected HostEntry create(String key) {
                return VpnModel.this.hostEntryDao.getEntry(key);
            }
        };
        this.logs = new LinkedHashSet<>();
        this.recordingLogs = false;
        this.requestCount = 0;
        this.applied.postValue(VpnServiceControls.isRunning(context));
        LocalBroadcastManager.getInstance(context).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Serializable extra = intent.getSerializableExtra(VpnService.VPN_UPDATE_STATUS_EXTRA);
                if (extra instanceof VpnStatus) {
                    VpnStatus status = (VpnStatus) extra;
                    applied.postValue(status == VpnStatus.RUNNING);
                }
            }
        }, new IntentFilter(VpnService.VPN_UPDATE_STATUS_INTENT));
        AppExecutors.getInstance().diskIO().execute(this::reloadWildcardRules);
    }

    @Override
    public AdBlockMethod getMethod() {
        return VPN;
    }

    @Override
    public void apply() throws HostErrorException {
        // Clear cache
        this.blockCache.evictAll();
        reloadWildcardRules();
        // Start VPN
        boolean started = VpnServiceControls.start(this.context);
        this.applied.postValue(started);
        if (!started) {
            throw new HostErrorException(ENABLE_VPN_FAIL);
        }
        setState(R.string.status_vpn_configuration_updated);
    }

    @Override
    public void revert() {
        VpnServiceControls.stop(this.context);
        this.applied.postValue(false);
    }

    @Override
    public boolean isRecordingLogs() {
        return this.recordingLogs;
    }

    @Override
    public void setRecordingLogs(boolean recording) {
        this.recordingLogs = recording;
    }

    @Override
    public List<String> getLogs() {
        return new ArrayList<>(this.logs);
    }

    @Override
    public void clearLogs() {
        this.logs.clear();
    }

    /**
     * Checks host entry related to an host name.
     *
     * @param host A hostname to check.
     * @return The related host entry.
     */
    public HostEntry getEntry(String host) {
        // Compute miss rate periodically
        this.requestCount++;
        if (this.requestCount >= 1000) {
            int hits = this.blockCache.hitCount();
            int misses = this.blockCache.missCount();
            double missRate = 100D * (hits + misses) / misses;
            Timber.d("Host cache miss rate: %s.", missRate);
            this.requestCount = 0;
        }
        // Add host to logs
        if (this.recordingLogs) {
            this.logs.add(host);
        }
        if (host == null) {
            return null;
        }
        // Check exact match in cache
        HostEntry entry = this.blockCache.get(host);
        if (entry != null) {
            return entry;
        }
        // If host was explicitly allowed, do not block subdomains
        if (this.hostListItemDao.getTypeOfHost(host) == ListType.ALLOWED) {
            return null;
        }
        // Check parent domains for wildcard/subdomain matching
        int lastDotIndex = host.lastIndexOf('.');
        int dotIndex = host.indexOf('.');
        while (dotIndex != -1 && dotIndex < lastDotIndex) {
            String parent = host.substring(dotIndex + 1);
            if (this.hostListItemDao.getTypeOfHost(parent) == ListType.ALLOWED) {
                break;
            }
            HostEntry parentEntry = this.blockCache.get(parent);
            if (parentEntry != null) {
                this.blockCache.put(host, parentEntry);
                return parentEntry;
            }
            dotIndex = host.indexOf('.', dotIndex + 1);
        }

        // Check compiled wildcard rules (* and ? patterns)
        for (CompiledWildcardRule rule : this.wildcardRules) {
            if (rule.pattern.matcher(host).matches()) {
                if (rule.type == ListType.ALLOWED) {
                    return null;
                }
                HostEntry wildcardEntry = new HostEntry();
                wildcardEntry.setHost(host);
                wildcardEntry.setType(rule.type);
                wildcardEntry.setRedirection(rule.redirection);
                this.blockCache.put(host, wildcardEntry);
                return wildcardEntry;
            }
        }
        return null;
    }

    public void reloadWildcardRules() {
        List<HostListItem> rules = this.hostListItemDao.getWildcardRules();
        List<CompiledWildcardRule> compiled = new ArrayList<>();
        for (HostListItem item : rules) {
            try {
                String regex = RegexUtils.wildcardToRegex(item.getHost());
                Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                compiled.add(new CompiledWildcardRule(p, item.getType(), item.getRedirection()));
            } catch (Exception e) {
                Timber.w(e, "Failed to compile wildcard rule %s", item.getHost());
            }
        }
        this.wildcardRules = compiled;
    }
}
