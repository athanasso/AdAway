package org.adaway.tile;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.lifecycle.LiveData;

import org.adaway.AdAwayApplication;
import org.adaway.helper.PreferenceHelper;
import org.adaway.model.adblocking.AdBlockMethod;
import org.adaway.model.adblocking.AdBlockModel;
import org.adaway.model.error.HostErrorException;
import org.adaway.ui.home.HomeActivity;
import org.adaway.util.AppExecutors;

import java.util.concurrent.atomic.AtomicBoolean;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;

import timber.log.Timber;

/**
 * This class is a {@link TileService} to toggle ad-blocking.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class AdBlockingTileService extends TileService {
    private final AtomicBoolean toggling = new AtomicBoolean(false);

    @Override
    public void onTileAdded() {
        boolean adBlocked = Boolean.TRUE.equals(getModel().isApplied().getValue());
        updateTile(adBlocked);
    }

    @Override
    public void onStartListening() {
        LiveData<Boolean> applied = getModel().isApplied();
        applied.observeForever(this::updateTile);
    }

    @Override
    public void onStopListening() {
        LiveData<Boolean> applied = getModel().isApplied();
        applied.removeObserver(this::updateTile);
    }

    @Override
    public void onClick() {
        AppExecutors.getInstance()
                .diskIO()
                .execute(this::toggleAdBlocking);
    }

    private void updateTile(boolean adBlocked) {
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(adBlocked ? STATE_ACTIVE : STATE_INACTIVE);
            tile.updateTile();
        }
    }

    private void openApp() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            startActivityAndCollapse(pendingIntent);
        } else {
            startActivityAndCollapse(intent);
        }
    }

    private void toggleAdBlocking() {
        if (this.toggling.get()) {
            return;
        }
        AdBlockModel model = getModel();
        try {
            this.toggling.set(true);
            if (Boolean.TRUE.equals(model.isApplied().getValue())) {
                model.revert();
            } else {
                if (PreferenceHelper.getAdBlockMethod(this) == AdBlockMethod.VPN
                        && android.net.VpnService.prepare(this) != null) {
                    openApp();
                    return;
                }
                model.apply();
            }
        } catch (HostErrorException e) {
            Timber.w(e, "Failed to toggle ad-blocking.");
            openApp();
        } catch (Exception e) {
            Timber.w(e, "Unexpected error toggling ad-blocking.");
            openApp();
        } finally {
            this.toggling.set(false);
        }
    }

    private AdBlockModel getModel() {
        return ((AdAwayApplication) getApplication()).getAdBlockModel();
    }
}
