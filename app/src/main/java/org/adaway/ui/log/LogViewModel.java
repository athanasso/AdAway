package org.adaway.ui.log;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.adaway.AdAwayApplication;
import org.adaway.db.AppDatabase;
import org.adaway.db.dao.HostEntryDao;
import org.adaway.db.dao.HostListItemDao;
import org.adaway.db.entity.HostListItem;
import org.adaway.db.entity.ListType;
import org.adaway.model.adblocking.AdBlockMethod;
import org.adaway.model.adblocking.AdBlockModel;
import org.adaway.model.error.HostErrorException;
import org.adaway.model.source.SourceModel;
import org.adaway.util.AppExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import timber.log.Timber;

import static org.adaway.db.entity.HostsSource.USER_SOURCE_ID;

/**
 * This class is an {@link AndroidViewModel} for the {@link LogActivity}.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class LogViewModel extends AndroidViewModel {
    private final AdBlockModel adBlockModel;
    private final SourceModel sourceModel;
    private final HostListItemDao hostListItemDao;
    private final HostEntryDao hostEntryDao;
    private final MutableLiveData<List<LogEntry>> logEntries;
    private final MutableLiveData<Boolean> recording;
    private LogEntrySort sort;
    private List<LogEntry> allLogs = Collections.emptyList();
    private String filterQuery = "";

    public LogViewModel(@NonNull Application application) {
        super(application);
        AdAwayApplication awayApplication = (AdAwayApplication) application;
        this.adBlockModel = awayApplication.getAdBlockModel();
        this.sourceModel = awayApplication.getSourceModel();
        this.hostListItemDao = AppDatabase.getInstance(application).hostsListItemDao();
        this.hostEntryDao = AppDatabase.getInstance(application).hostEntryDao();
        this.logEntries = new MutableLiveData<>();
        this.recording = new MutableLiveData<>(this.adBlockModel.isRecordingLogs());
        this.sort = LogEntrySort.TOP_LEVEL_DOMAIN;
    }

    public boolean areBlockedRequestsIgnored() {
        return this.adBlockModel.getMethod() == AdBlockMethod.ROOT;
    }

    public LiveData<List<LogEntry>> getLogs() {
        return this.logEntries;
    }

    public void clearLogs() {
        this.adBlockModel.clearLogs();
        this.allLogs = Collections.emptyList();
        this.logEntries.postValue(Collections.emptyList());
    }

    public void updateLogs() {
        AppExecutors.getInstance().diskIO().execute(
                () -> {
                    // Get tcpdump logs
                    List<LogEntry> logItems = this.adBlockModel.getLogs()
                            .parallelStream()
                            .map(log -> {
                                ListType type = this.hostEntryDao.getTypeOfHost(log);
                                if (type == null) {
                                    type = this.hostListItemDao.getTypeOfHost(log);
                                }
                                return new LogEntry(log, type);
                            })
                            .sorted(this.sort.comparator())
                            .collect(Collectors.toList());
                    // Post result
                    this.allLogs = logItems;
                    applyFilter();
                }
        );
    }

    public void toggleSort() {
        this.sortDnsRequests(this.sort == LogEntrySort.ALPHABETICAL ?
                LogEntrySort.TOP_LEVEL_DOMAIN :
                LogEntrySort.ALPHABETICAL
        );
    }

    public LiveData<Boolean> isRecording() {
        return this.recording;
    }

    public void toggleRecording() {
        boolean recording = !this.adBlockModel.isRecordingLogs();
        this.adBlockModel.setRecordingLogs(recording);
        this.recording.postValue(recording);
    }

    public void addListItem(@NonNull String host, @NonNull ListType type, String redirection) {
        // Create new host list item
        HostListItem item = new HostListItem();
        item.setType(type);
        item.setHost(host);
        item.setRedirection(redirection);
        item.setEnabled(true);
        item.setSourceId(USER_SOURCE_ID);
        // Insert or update host list item, sync host entries and apply
        AppExecutors.getInstance().diskIO().execute(() -> {
            Optional<Integer> id = this.hostListItemDao.getHostId(host);
            if (id.isPresent()) {
                item.setId(id.get());
                this.hostListItemDao.update(item);
            } else {
                this.hostListItemDao.insert(item);
            }
            try {
                this.sourceModel.syncHostEntries();
                this.adBlockModel.apply();
            } catch (HostErrorException exception) {
                Timber.w(exception, "Failed to apply ad block model after adding host: %s", host);
            }
        });
        // Update log entries
        updateLogEntryType(host, type);
    }

    public void removeListItem(@NonNull String host) {
        // Delete host list item, sync host entries and apply
        AppExecutors.getInstance().diskIO().execute(() -> {
            this.hostListItemDao.deleteUserFromHost(host);
            try {
                this.sourceModel.syncHostEntries();
                this.adBlockModel.apply();
            } catch (HostErrorException exception) {
                Timber.w(exception, "Failed to apply ad block model after removing host: %s", host);
            }
        });
        // Update log entries
        updateLogEntryType(host, null);
    }

    public void setFilterQuery(String query) {
        this.filterQuery = query == null ? "" : query.trim().toLowerCase();
        applyFilter();
    }

    private void applyFilter() {
        if (this.filterQuery.isEmpty()) {
            this.logEntries.postValue(this.allLogs);
        } else {
            List<LogEntry> filtered = this.allLogs.stream()
                    .filter(entry -> entry.getHost().toLowerCase().contains(this.filterQuery))
                    .collect(Collectors.toList());
            this.logEntries.postValue(filtered);
        }
    }

    private void updateLogEntryType(@NonNull String host, ListType type) {
        this.allLogs = this.allLogs.stream()
                .map(entry -> entry.getHost().equals(host) ? new LogEntry(host, type) : entry)
                .collect(Collectors.toList());
        applyFilter();
    }

    private void sortDnsRequests(LogEntrySort sort) {
        // Save current sort
        this.sort = sort;
        // Apply sort to values
        if (!this.allLogs.isEmpty()) {
            List<LogEntry> sortedEntries = new ArrayList<>(this.allLogs);
            sortedEntries.sort(this.sort.comparator());
            this.allLogs = sortedEntries;
            applyFilter();
        }
        // Notify user
        Toast.makeText(
                getApplication(),
                this.sort.getName(),
                Toast.LENGTH_SHORT
        ).show();
    }
}
