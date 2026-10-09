/*
 * Copyright (C) 2011-2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This file is part of AdAway.
 *
 * AdAway is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdAway is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdAway.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.adaway.ui.log;

import static org.adaway.ui.Animations.hideView;
import static org.adaway.ui.Animations.showView;
import static java.lang.Boolean.TRUE;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.adaway.R;
import org.adaway.databinding.LogActivityBinding;
import org.adaway.databinding.LogRedirectDialogBinding;
import org.adaway.db.entity.ListType;
import org.adaway.helper.ThemeHelper;
import org.adaway.ui.dialog.AlertDialogValidator;
import org.adaway.util.Clipboard;
import org.adaway.util.RegexUtils;

import java.util.List;

/**
 * This class is an {@link android.app.Activity} to show DNS request log entries.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class LogActivity extends AppCompatActivity implements LogViewCallback {
    private LogActivityBinding binding;
    /**
     * The view model (<code>null</code> if activity is not created).
     */
    private LogViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
         * Create activity.
         */
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        this.binding = LogActivityBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // Get view model
        this.mViewModel = new ViewModelProvider(this).get(LogViewModel.class);
        /*
         * Configure swipe layout.
         */
        this.binding.swipeRefresh.setOnRefreshListener(this.mViewModel::updateLogs);
        /*
         * Configure empty view.
         */
        if (this.mViewModel.areBlockedRequestsIgnored()) {
            this.binding.emptyTextView.append(getString(R.string.log_blocked_requests_ignored));
        }
        /*
         * Configure recycler view.
         */
        // Get recycler view
        this.binding.logList.setHasFixedSize(true);
        // Defile recycler layout
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        this.binding.logList.setLayoutManager(linearLayoutManager);
        // Create recycler adapter
        LogAdapter adapter = new LogAdapter(this);
        this.binding.logList.setAdapter(adapter);
        /*
         * Configure fab.
         */
        this.binding.toggleLogRecording.setOnClickListener(v -> this.mViewModel.toggleRecording());
        this.mViewModel.isRecording().observe(this, recoding ->
                this.binding.toggleLogRecording.setImageResource(TRUE.equals(recoding) ?
                        R.drawable.ic_pause_24dp :
                        R.drawable.ic_record_24dp
                )
        );

        /*
         * Load data.
         */
        // Bind view model to the list view
        this.mViewModel.getLogs().observe(this, logEntries -> {
            if (logEntries.isEmpty()) {
                showView(this.binding.emptyTextView);
            } else {
                hideView(this.binding.emptyTextView);
            }
            adapter.submitList(logEntries);
            this.binding.swipeRefresh.setRefreshing(false);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mark as loading data
        this.binding.swipeRefresh.setRefreshing(true);
        // Load initial data
        this.mViewModel.updateLogs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.log_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint(getString(R.string.lists_menu_filter));
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        LogActivity.this.mViewModel.setFilterQuery(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        LogActivity.this.mViewModel.setFilterQuery(newText);
                        return true;
                    }
                });
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.share) {
            shareLogs();
            return true;
        } else if (item.getItemId() == R.id.sort) {
            this.mViewModel.toggleSort();
            return true;
        } else if (item.getItemId() == R.id.delete) {
            this.mViewModel.clearLogs();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareLogs() {
        if (this.mViewModel == null) {
            return;
        }
        List<LogEntry> entries = this.mViewModel.getLogs().getValue();
        if (entries == null || entries.isEmpty()) {
            Toast.makeText(this, R.string.tcpdump_no_logs_to_share, Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (LogEntry entry : entries) {
            builder.append(entry.getHost()).append('\n');
        }
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, builder.toString().trim());
        sendIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(sendIntent, getString(R.string.tcpdump_menu_share));
        startActivity(shareIntent);
    }

    @Override
    public void addListItem(@NonNull String hostName, @NonNull ListType type) {
        // Check view model
        if (this.mViewModel == null) {
            return;
        }
        // Check type other than redirection
        if (type != ListType.REDIRECTED) {
            // Insert list item
            this.mViewModel.addListItem(hostName, type, null);
            int messageRes = (type == ListType.BLOCKED) ? R.string.log_domain_blocked : R.string.log_domain_allowed;
            Snackbar.make(this.binding.coordinator, getString(messageRes, hostName), Snackbar.LENGTH_LONG)
                    .setAction(R.string.log_button_undo, v -> removeListItem(hostName))
                    .show();
        } else {
            // Create dialog view
            LayoutInflater inflater = LayoutInflater.from(this);
            LogRedirectDialogBinding redirectBinding = LogRedirectDialogBinding.inflate(inflater);
            // Create dialog
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                    .setCancelable(true)
                    .setTitle(R.string.log_redirect_dialog_title)
                    .setView(redirectBinding.getRoot())
                    // Setup buttons
                    .setPositiveButton(
                            R.string.button_add,
                            (dialog, which) -> {
                                // Close dialog
                                dialog.dismiss();
                                // Check IP is valid
                                String ip = redirectBinding.redirectIp.getText().toString();
                                if (RegexUtils.isValidIP(ip)) {
                                    // Insert list item
                                    this.mViewModel.addListItem(hostName, type, ip);
                                    Snackbar.make(this.binding.coordinator, getString(R.string.log_domain_blocked, hostName), Snackbar.LENGTH_LONG)
                                            .setAction(R.string.log_button_undo, v -> removeListItem(hostName))
                                            .show();
                                }
                            }
                    )
                    .setNegativeButton(
                            R.string.button_cancel,
                            (dialog, which) -> dialog.dismiss()
                    )
                    .create();
            // Show dialog
            alertDialog.show();
            // Set button validation behavior
            redirectBinding.redirectIp.addTextChangedListener(
                    new AlertDialogValidator(alertDialog, RegexUtils::isValidIP, false)
            );
        }
    }

    @Override
    public void removeListItem(@NonNull String hostName) {
        if (this.mViewModel != null) {
            this.mViewModel.removeListItem(hostName);
            Snackbar.make(this.binding.coordinator, getString(R.string.log_domain_removed, hostName), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onHostClick(@NonNull LogEntry entry) {
        String host = entry.getHost();
        CharSequence[] options = new CharSequence[]{
                getString(R.string.log_action_block),
                getString(R.string.log_action_allow),
                getString(R.string.log_action_copy),
                getString(R.string.log_action_browser)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(host)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            addListItem(host, ListType.BLOCKED);
                            break;
                        case 1:
                            addListItem(host, ListType.ALLOWED);
                            break;
                        case 2:
                            copyHostToClipboard(host);
                            break;
                        case 3:
                            openHostInBrowser(host);
                            break;
                    }
                })
                .show();
    }

    @Override
    public void openHostInBrowser(@NonNull String hostName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://" + hostName));
        startActivity(intent);
    }

    @Override
    public void copyHostToClipboard(@NonNull String hostName) {
        Clipboard.copyHostToClipboard(this, hostName);
    }
}
