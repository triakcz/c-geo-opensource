package cgeo.geocaching.downloadservice;

import cgeo.geocaching.DownloadManagerActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.HttpResponse;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CacheDownloadService extends Service {

    private volatile boolean downloadTaskRunning = false;
    private volatile boolean send2CgeoRunning = false;
    private QueueItem actualCache;

    private RemoteCallbackList<ICacheDownloadServiceCallback> callbackList = new RemoteCallbackList<ICacheDownloadServiceCallback>();

    public static final int CGEO_DOWNLOAD_NOTIFICATION_ID = 91258;

    public static final String EXTRA_LIST_ID = "LIST_ID";
    public static final String EXTRA_GEOCODE = "GEOCODE";
    public static final String EXTRA_REFRESH = "REFRESH";
    public static final String EXTRA_SEND2CGEO = "SEND2CGEO";

    private int send2cgeostartId;

    private int downloadedCaches = 0;

    LinkedBlockingQueue<QueueItem> queue = new LinkedBlockingQueue<QueueItem>();

    private enum OperationType {
        STORE,
        REFRESH
    }

    /**
     * item container for processing queue
     */
    private class QueueItem {
        private final String geocode;
        private final int startId;
        private final int listId;
        private final OperationType operation;

        public QueueItem(String cacheCode, int startId, int listId, OperationType type) {
            this.startId = startId;
            this.geocode = cacheCode;
            this.listId = listId;
            this.operation = type;
        }

        public QueueItem(String geocode) {
            this(geocode, 0, 0, OperationType.STORE);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((geocode == null) ? 0 : geocode.hashCode());
            result = prime * result + startId;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            QueueItem other = (QueueItem) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (geocode == null) {
                if (other.geocode != null) {
                    return false;
                }
            } else if (!geocode.equals(other.geocode)) {
                return false;
            }
            return true;
        }

        private CacheDownloadService getOuterType() {
            return CacheDownloadService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getExtras() != null) {
                if (intent.hasExtra(EXTRA_SEND2CGEO)) {
                    Log.d("SEND2CGEO request intent received");
                    startSend2Cgeo(startId);
                } else if (null != intent.getExtras().getString(EXTRA_GEOCODE)) {
                    int listId = intent.getIntExtra(EXTRA_LIST_ID, StoredList.STANDARD_LIST_ID);
                    QueueItem item =
                            new QueueItem(intent.getExtras().getString(EXTRA_GEOCODE),
                                    startId,
                                    listId,
                                    intent.getExtras().containsKey(EXTRA_REFRESH) ? OperationType.REFRESH : OperationType.STORE);
                    try {
                        if (!queue.contains(item)) {
                            queue.put(item);
                            notifyChanges();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (!downloadTaskRunning) {
                        downloadTaskRunning = true;
                        new DownloadCachesTask().execute();
                    }
                }
            }
        }
        return START_STICKY;
    }

    private void startSend2Cgeo(int startId) {
        if (send2CgeoRunning == false) {
            send2CgeoRunning = true;
            send2cgeostartId = startId;
            ActivityMixin.showShortToast(this, getString(R.string.download_service_send2cgeostart));
            new Send2CgeoRequestTask().execute();
        } else {
            ActivityMixin.showShortToast(this, getString(R.string.download_service_send2cgeo_is_running));
        }
    }

    public static final int SEND2CGEO_WAITING = 0;
    public static final int SEND2CGEO_DOWNLOAD_START = 1;
    public static final int SEND2CGEO_DOWNLOAD_FAILED = 2;
    public static final int SEND2CGEO_REGISTER = 3;
    public static final int SEND2CGEO_DONE = 4;

    class Send2CgeoRequestTask extends AsyncTask<Void, String, Void> {
        volatile boolean needToStop = false;
        int ret = SEND2CGEO_DONE;

        @Override
        protected Void doInBackground(Void... arg0) {

            int delay = -1;
            int times = 0;

            while (!needToStop && times < 3 * 60 / 5) // maximum: 3 minutes, every 5 seconds
            {
                //download new code
                String deviceCode = Settings.getWebDeviceCode();
                if (deviceCode == null) {
                    deviceCode = "";
                }
                final Parameters params = new Parameters("code", deviceCode);
                HttpResponse responseFromWeb = Network.getRequest("http://send2.cgeo.org/read.html", params);

                if (responseFromWeb != null && responseFromWeb.getStatusLine().getStatusCode() == 200) {
                    final String response = Network.getResponseData(responseFromWeb);
                    if (response.length() > 2) {
                        delay = 1;

                        Intent i = new Intent(CacheDownloadService.this, CacheDownloadService.class);
                        i.putExtra(CacheDownloadService.EXTRA_GEOCODE, response);
                        //TODO: put list ID ... which ?
                        startService(i);
                        notifySend2CgeoStatus(SEND2CGEO_DOWNLOAD_START, response);
                        publishProgress(getString(R.string.download_service_queued_cache, response));
                    } else if ("RG".equals(response)) {
                        //Server returned RG (registration) and this device no longer registered.
                        Settings.setWebNameCode(null, null);
                        needToStop = true;
                        ret = SEND2CGEO_REGISTER;
                        publishProgress(getString(R.string.sendToCgeo_no_registration));
                        break;
                    } else {
                        delay = 0;
                        notifySend2CgeoStatus(SEND2CGEO_WAITING);
                    }
                }
                if (responseFromWeb == null || responseFromWeb.getStatusLine().getStatusCode() != 200) {
                    needToStop = true;
                    ret = SEND2CGEO_DOWNLOAD_FAILED;
                    break;

                }

                if (delay == 0)
                {
                    sleep(5000); //No caches 5s
                    times++;
                } else {
                    sleep(500); //Cache was loaded 0.5s
                    times = 0;
                }

            }

            notifySend2CgeoStatus(ret);
            if (ret == SEND2CGEO_DONE) {
                publishProgress(getString(R.string.download_service_send2cgefinished));
            }
            send2CgeoRunning = false;
            if (actualCache != null && actualCache.startId > send2cgeostartId) {
                stopSelf(send2cgeostartId);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            ActivityMixin.showShortToast(CacheDownloadService.this, values[0]);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Used to notify connected DownloadManagerActivity via callback
     *
     * @param finish
     *            - true for sending finish event, otherwise refresh
     */
    private void notifyClients(boolean finish) {
        synchronized (callbackList) {
            final int N = callbackList.beginBroadcast();
            for (int i = 0; i < N; i++) {
                try {
                    ICacheDownloadServiceCallback cb = callbackList.getBroadcastItem(i);
                    if (finish) {
                        cb.notifyFinish();
                    } else {
                        cb.notifyRefresh();
                    }
                } catch (RemoteException e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                }
            }
            callbackList.finishBroadcast();
        }
    }

    private void notifySend2CgeoStatus(int status) {
        notifySend2CgeoStatus(status, null);
    }

    private void notifySend2CgeoStatus(int status, String geocode) {
        synchronized (callbackList) {
            final int N = callbackList.beginBroadcast();
            for (int i = 0; i < N; i++) {
                try {
                    ICacheDownloadServiceCallback cb = callbackList.getBroadcastItem(i);
                    cb.notifySend2CgeoStatus(status, geocode);
                } catch (RemoteException e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                }
            }
            callbackList.finishBroadcast();
        }
    }

    /**
     * Used to notify client that state has changed
     *
     * @param finish
     *            - true for finishing of app
     */
    private void notifyChanges(boolean finish) {
        notifyClients(finish);
        showNotification();
    }

    /**
     * @see notifyCahnges
     *      with default finish value (false)
     */
    private synchronized void notifyChanges() {
        notifyChanges(false);
    }

    /**
     * display statusbar notification depending on service state
     */
    @TargetApi(5)
    private void showNotification() {
        String ticker = (actualCache == null) ? "DownloadService idle." : "Downloading " + actualCache.geocode;
        Notification notification = new Notification(R.drawable.icon_sync, ticker, System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, DownloadManagerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        StringBuilder status = new StringBuilder();
        if (actualCache != null) {
            status.append("Downloading ");
            status.append(actualCache.geocode);
            status.append(", queue size " + queue.size());
        }
        notification.setLatestEventInfo(getApplicationContext(), "Download service", status.toString(), contentIntent);
        startForeground(CGEO_DOWNLOAD_NOTIFICATION_ID, notification);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("CACHEDOWNLOADSERVICE START");
        notifyChanges();
        new DownloadCachesTask().execute();
    }

    /**
     * Transforms queue to array of geocodes
     *
     * @return transformed array
     */
    public String[] queueAsArray() {
        Object[] queueAsArray = queue.toArray();
        String[] result = new String[queueAsArray.length];

        for (int i = 0; i < queueAsArray.length; i++) {
            result[i] = ((QueueItem) (queueAsArray[i])).geocode;
        }
        return result;
    }

    /**
     * IPC interface for binding the service
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return new ICacheDownloadService.Stub() {
            @Override
            public int queueStatus() throws RemoteException {
                return queue.size();
            }

            @Override
            public String[] queuedCodes() throws RemoteException {
                return queueAsArray();
            }

            @Override
            public String actualDownload() throws RemoteException {
                if (actualCache != null) {
                    return actualCache.geocode;
                }
                return null;
            }

            @Override
            public void registerStatusCallback(ICacheDownloadServiceCallback cdsc) throws RemoteException {
                if (cdsc != null) {
                    callbackList.register(cdsc);
                }
            }

            @Override
            public void unregisterStatusCallback(ICacheDownloadServiceCallback cdsc) throws RemoteException {
                if (cdsc != null) {
                    callbackList.unregister(cdsc);
                }
            }

            @Override
            public void removeFromQueue(String geocode) throws RemoteException {
                if (queue.remove(new QueueItem(geocode))) {
                    notifyClients(false);

                    ActivityMixin.showShortToast(CacheDownloadService.this, getString(R.string.download_service_removed_cache, geocode));
                } else {
                    ActivityMixin.showShortToast(CacheDownloadService.this, getString(R.string.download_service_not_removed_cache, geocode));
                }
            }

            @Override
            public void flushQueueAndStopService() throws RemoteException {
                queue.clear();
                notifyClients(true);
                ActivityMixin.showShortToast(CacheDownloadService.this, getString(R.string.download_service_flushed_queue_and_exiting));
                CacheDownloadService.this.stopSelf();
            }

            @Override
            public void pauseDownloading() throws RemoteException {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("NOT IMPLEMENTED");
            }

            @Override
            public void resumeDownloading() throws RemoteException {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("NOT IMPLEMENTED");
            }
        };
    }

    @Override
    public void onDestroy() {
        Log.d("CACHEDOWNLOADSERVICE STOP");
        notifyClients(true);
        callbackList.kill();
        super.onDestroy();
    }

    /**
     * Background downloading of caches and queue processing
     */
    private class DownloadCachesTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                do {
                    actualCache = queue.poll(15, TimeUnit.SECONDS);
                    //Random wait between caches to prevent hogging of servers
                    Thread.sleep((long) (Math.random() * 5000) + 1000);
                    if (actualCache != null) {
                        Log.d("CACHEDOWNLOAD STARTING DOWNLOAD" + actualCache.geocode);
                        notifyChanges();
                        cgCache cache = new cgCache();
                        cache.setGeocode(actualCache.geocode);
                        cache.setListId(actualCache.listId);

                        switch (actualCache.operation) {
                            case STORE:
                                //TODO: use handler in the future
                                cache.store(null);
                                break;
                            case REFRESH:
                                //TODO: use handler in the future
                                cache.refresh(actualCache.listId, null);
                                break;

                        }

                        // we need most recent start Id to kill service (do not ack for
                        // download if it is newer as send2cgeo thread, save for future use)
                        if (!send2CgeoRunning) {
                            stopSelf(actualCache.startId);
                        } else if (send2cgeostartId < actualCache.startId) {
                            send2cgeostartId = actualCache.startId;
                        }

                        Log.d("CACHEDOWNLOAD FINISHED DOWNLOAD" + actualCache.geocode);
                        downloadedCaches++;
                        notifyChanges(queue.isEmpty());
                    }
                } while (actualCache != null || send2CgeoRunning);
                downloadTaskRunning = false;
            } catch (InterruptedException e) {
            }
            return null;
        }
    }
}