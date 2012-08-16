package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.downloadservice.CacheDownloadService;
import cgeo.geocaching.downloadservice.ICacheDownloadService;
import cgeo.geocaching.downloadservice.ICacheDownloadServiceCallback;
import cgeo.geocaching.ui.CacheQueueAdapter;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

public class DownloadManagerActivity extends AbstractActivity {

    private static final int DS_CONNECTED = 35429050;
    private static final int DS_UPDATE = 35429051;
    private static final int DS_FINISHED = 35429052;

    private static final String SEND_2_CGEO_EXTRA_STATUS = "GEOCODE";

    CacheDownloadServiceConnection cdsConnection = new CacheDownloadServiceConnection();
    ICacheDownloadService downloadService;
    boolean serviceIsBound = false;
    private CacheQueueAdapter adapter;
    private LinearLayout loadFromWebLayout;
    private ProgressBar loadFromWebProgress;
    private TextView loadFromWebText;

    /**
     * Handles events from callbacks
     */
    final Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case DS_CONNECTED:
                case DS_UPDATE:
                    try {
                        String caches[] = downloadService.queuedCodes();
                        ArrayList<String> cacheList = new ArrayList<String>(caches.length);
                        for (String c : caches) {
                            cacheList.add(c);
                        }
                        adapter = new CacheQueueAdapter(DownloadManagerActivity.this, cacheList);

                        ListView lv = (ListView) findViewById(R.id.downloadlistview);
                        lv.setAdapter(adapter);
                        View actualDownloadView = findViewById(R.id.actualDownload);
                        adapter.setView(actualDownloadView, downloadService.actualDownload(), false);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case DS_FINISHED:
                    View actualDownloadView = findViewById(R.id.actualDownload);
                    TextView text = (TextView) (actualDownloadView.findViewById(R.id.text));
                    text.setText(R.string.download_service_finished);
                    text.setCompoundDrawables(null, null, null, null);
                    TextView info = (TextView) (actualDownloadView.findViewById(R.id.info));
                    info.setText("");
                    adapter.clear();
                    break;

                case CacheDownloadService.SEND2CGEO_DONE:
                case CacheDownloadService.SEND2CGEO_DOWNLOAD_FAILED:
                case CacheDownloadService.SEND2CGEO_REGISTER:
                    loadFromWebLayout.setVisibility(View.GONE);
                    loadFromWebProgress.setVisibility(View.GONE);
                    break;
                case CacheDownloadService.SEND2CGEO_DOWNLOAD_START:
                case CacheDownloadService.SEND2CGEO_WAITING:
                    loadFromWebLayout.setVisibility(View.VISIBLE);
                    loadFromWebProgress.setVisibility(View.VISIBLE);
                    switch (msg.what) {
                        case CacheDownloadService.SEND2CGEO_WAITING:
                            loadFromWebText.setText(getString(R.string.web_import_waiting) + " " + msg.getData().getString(SEND_2_CGEO_EXTRA_STATUS));
                            break;
                        case CacheDownloadService.SEND2CGEO_DOWNLOAD_START:
                            loadFromWebText.setText(getString(R.string.download_service_queued_cache, msg.getData().getString(SEND_2_CGEO_EXTRA_STATUS)));
                            break;
                    }
                    break;
            }
        }
    };

    public void removeCacheFromQueue(String geocode) {
        if (downloadService != null) {
            try {
                downloadService.removeFromQueue(geocode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * constructor sets help topic
     */
    public DownloadManagerActivity() {
        super("c:geo-download-manager");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setContentView(R.layout.download_manager);
        loadFromWebLayout = (LinearLayout) findViewById(R.id.webDownloadBar);
        loadFromWebProgress = (ProgressBar) loadFromWebLayout.findViewById(R.id.webDownloadProgress);
        loadFromWebText = (TextView) loadFromWebLayout.findViewById(R.id.webDownloadText);
    }

    public static final int MENU_FLUSH = 1125;
    public static final int MENU_PAUSE = 1126;
    public static final int MENU_RESUME = 1127;
    public static final int MENU_DOWNLOAD_FROM_WEB = 1128;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_FLUSH, 0, getString(R.string.download_service_flush));
        menu.add(0, MENU_PAUSE, 0, getString(R.string.download_service_pause));
        menu.add(0, MENU_RESUME, 0, getString(R.string.download_service_resume));
        menu.add(0, MENU_DOWNLOAD_FROM_WEB, 0, getString(R.string.web_import_title));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_FLUSH:
                if (downloadService != null) {
                    try {
                        downloadService.flushQueueAndStopService();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                return true;

            case MENU_PAUSE:
                return true;

            case MENU_RESUME:
                return true;

            case MENU_DOWNLOAD_FROM_WEB:
                Intent serviceIntent = new Intent(getApplicationContext(), CacheDownloadService.class);
                serviceIntent.putExtra(CacheDownloadService.EXTRA_SEND2CGEO, true);
                startService(serviceIntent);
                return true;
            default:
                return false;
        }
    }

    /**
     * binds service on starting/resuming activity
     */
    @Override
    protected void onResume() {
        super.onResume();
        ActivityMixin.showShortToast(this, "DownloadManager onResume");
        Intent service = new Intent(getApplicationContext(), CacheDownloadService.class);
        serviceIsBound = true;
        //default flags
        bindService(service, cdsConnection, BIND_AUTO_CREATE);
    }

    /**
     * unbinds service on pausing (to save some resources)
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (serviceIsBound) {
            if (downloadService != null) {
                try {
                    downloadService.unregisterStatusCallback(callback);
                } catch (RemoteException e) {
                    //service crash is possible, but not to be handled when exiting
                }
            }
            unbindService(cdsConnection);
            serviceIsBound = false;
        }
        finish();
    }

    /**
     * callback class called by android when connection to service is established and closed
     */

    private class CacheDownloadServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadService = (ICacheDownloadService) service;
            uiHandler.sendEmptyMessage(DS_CONNECTED);
            try {
                downloadService.registerStatusCallback(callback);
            } catch (RemoteException e) {
                //this shouldn't be fired when service is just connected
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            downloadService = null;
        }
    }

    /**
     * callback Stub to be notified from service on progress
     */
    private ICacheDownloadServiceCallback callback = new ICacheDownloadServiceCallback.Stub() {
        /**
         * called from service when data changed
         */
        @Override
        public void notifyRefresh() throws RemoteException {
            uiHandler.sendEmptyMessage(DS_UPDATE);
        }

        /**
         * called from service when download finished
         */
        @Override
        public void notifyFinish() throws RemoteException {
            uiHandler.sendEmptyMessage(DS_FINISHED);
        }

        @Override
        public void notifySend2CgeoStatus(int status, String geocode) throws RemoteException {
            Message msg = new Message();
            msg.what = status;
            Bundle bundle = new Bundle();
            bundle.putString(SEND_2_CGEO_EXTRA_STATUS, geocode);
            msg.setData(bundle);
            uiHandler.sendMessage(msg);
        }
    };
}