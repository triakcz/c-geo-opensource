package cgeo.geocaching.downloadservice;

interface ICacheDownloadServiceCallback {
    void notifyRefresh(); 
    void notifyFinish();
    void notifySend2CgeoStatus(int status, String geocode);
}
