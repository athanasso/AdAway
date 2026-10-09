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

package org.adaway.ui.hosts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.adaway.R;
import org.adaway.db.entity.HostsSource;
import org.adaway.ui.adblocking.ApplyConfigurationSnackbar;
import org.adaway.ui.source.SourceEditActivity;

import static org.adaway.ui.source.SourceEditActivity.SOURCE_ID;

/**
 * This class is a {@link Fragment} to display and manage hosts sources.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class HostsSourcesFragment extends Fragment implements HostsSourcesViewCallback {
    /**
     * The view model (<code>null</code> if view is not created).
     */
    private HostsSourcesViewModel mViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get activity
        Activity activity = requireActivity();
        // Initialize view model
        this.mViewModel = new ViewModelProvider(this).get(HostsSourcesViewModel.class);
        LifecycleOwner lifecycleOwner = getViewLifecycleOwner();
        // Create fragment view
        View view = inflater.inflate(R.layout.hosts_sources_fragment, container, false);
        /*
         * Configure snackbar.
         */
        // Get lists layout to attached snackbar to
        CoordinatorLayout coordinatorLayout = view.findViewById(R.id.coordinator);
        // Create apply snackbar
        ApplyConfigurationSnackbar applySnackbar = new ApplyConfigurationSnackbar(coordinatorLayout, true, true);
        // Bind snakbar to view models
        this.mViewModel.getHostsSources().observe(lifecycleOwner, applySnackbar.createObserver());
        /*
         * Configure recycler view.
         */
        // Store recycler view
        RecyclerView recyclerView = view.findViewById(R.id.hosts_sources_list);
        recyclerView.setHasFixedSize(true);
        // Defile recycler layout
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(activity);
        recyclerView.setLayoutManager(linearLayoutManager);
        // Create recycler adapter
        HostsSourcesAdapter adapter = new HostsSourcesAdapter(this);
        recyclerView.setAdapter(adapter);
        // Bind adapter to view model
        this.mViewModel.getHostsSources().observe(lifecycleOwner, adapter::submitList);
        /*
         * Add floating action button.
         */
        // Get floating action button
        FloatingActionButton button = view.findViewById(R.id.hosts_sources_add);
        // Set click listener to display menu add entry
        button.setOnClickListener(actionButton -> showAddSourceChoiceDialog());
        // Return fragment view
        return view;
    }

    private void showAddSourceChoiceDialog() {
        CharSequence[] options = new CharSequence[]{
                getString(R.string.source_dialog_add_custom),
                getString(R.string.source_dialog_add_presets)
        };
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.source_dialog_add_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        startSourceEdition(null);
                    } else if (which == 1) {
                        showCuratedPresetsDialog();
                    }
                })
                .show();
    }

    private static class PresetInfo {
        final String label;
        final String url;
        PresetInfo(String label, String url) {
            this.label = label;
            this.url = url;
        }
    }

    private static final PresetInfo[] PRESETS = new PresetInfo[]{
            new PresetInfo("Hâgezi Multi LIGHT", "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/hosts/light.txt"),
            new PresetInfo("OISD Basic", "https://small.oisd.nl"),
            new PresetInfo("StevenBlack Unified", "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"),
            new PresetInfo("AdGuard DNS Filter", "https://adguardteam.github.io/HostlistsRegistry/assets/filter_1.txt"),
            new PresetInfo("Dan Pollock / Someonewhocares", "https://someonewhocares.org/hosts/zero/hosts")
    };

    private void showCuratedPresetsDialog() {
        CharSequence[] items = new CharSequence[PRESETS.length];
        boolean[] checked = new boolean[PRESETS.length];
        for (int i = 0; i < PRESETS.length; i++) {
            items[i] = PRESETS[i].label + "\n" + PRESETS[i].url;
            checked[i] = true;
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.source_presets_title)
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.button_add, (dialog, which) -> {
                    java.util.List<HostsSource> toAdd = new java.util.ArrayList<>();
                    for (int i = 0; i < PRESETS.length; i++) {
                        if (checked[i]) {
                            HostsSource source = new HostsSource();
                            source.setLabel(PRESETS[i].label);
                            source.setUrl(PRESETS[i].url);
                            source.setEnabled(true);
                            toAdd.add(source);
                        }
                    }
                    if (!toAdd.isEmpty()) {
                        this.mViewModel.insertSources(toAdd);
                        android.widget.Toast.makeText(requireContext(), R.string.source_presets_added, android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    @Override
    public void toggleEnabled(HostsSource source) {
        this.mViewModel.toggleSourceEnabled(source);
    }

    @Override
    public void edit(HostsSource source) {
        startSourceEdition(source);
    }

    private void startSourceEdition(@Nullable HostsSource source) {
        Intent intent = new Intent(requireContext(), SourceEditActivity.class);
        if (source != null) {
            intent.putExtra(SOURCE_ID, source.getId());
        }
        startActivity(intent);
    }
}
