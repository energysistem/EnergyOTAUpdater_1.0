package es.energy.energyotaupdater;

import android.*;
import android.R;
import android.os.Handler;
import android.util.Log;
import android.webkit.DownloadListener;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.util.concurrent.ExecutorService;

/**
 * Created by DFV on 9/08/13.
 */
public class FileDownloadTask extends Thread {

    private class DownloadFilePiece implements Runnable
    {
        private long endPosition;
        private File file;
        private boolean isRange;
        private es.energy.energyotaupdater.FileDownloadTask.DownloadListener dListener;
        private int pieceId;
        private long posNow;
        private long startPosition;


        private void ErrorOccurre(int i, long l)
        {
            if (dListener!=null)
            {
                dListener.onErrorOcurre(i,l);
            }
        }

        private void afterPerBlockDown(int i, int j, long l)
        {
            if (dListener!=null)
            {
                dListener.onPerBlockDown(i,j,l);
            }
        }

        private void onePieceComplete()
        {
            if (dListener!=null)
            {
                dListener.onPieceComplete();
            }
        }

        @Override
        public void run() {
            HttpGet httpget;
            HttpResponse httpresponse;
            int statusCode;
            Header header[];
            int headerLength;
            httpget= new HttpGet(downloadUri);
            if(isRange)
            {
                httpget.addHeader("Range", "bytes=" + posNow + "-" + endPosition);
            }
            try
            {
                httpresponse = httpClient.execute(httpget);
                statusCode=httpresponse.getStatusLine().getStatusCode();
                if(!debug)
                {

                }
                header=httpresponse.getAllHeaders();
                headerLength=header.length;
                RandomAccessFile randomAccessFile;
                int cont=0;
                do {
                    if(cont >= headerLength)
                        break;
                    Header headertemp = header[cont];
                    cont++;
                }while(true);
                InputStream inputstream;
                int tamanyo;
                byte abyte0[];
                if(statusCode==206)
                {

                }
                if (statusCode!=200)
                {

                }
                if (isRange)
                {

                }
                inputstream = httpresponse.getEntity().getContent();
                randomAccessFile = new RandomAccessFile(file,"rw");
                randomAccessFile.seek(posNow);
                abyte0=new byte[4096];
                tamanyo=inputstream.read(abyte0,0,abyte0.length);
                if(tamanyo <= 0)
                {
                    Log.d("EnergyOTA","Error inputstream menor que 0");
                }
                if(Thread.interrupted())
                {
                    httpget.abort();
                    return;
                }
                randomAccessFile.write(abyte0,0,tamanyo);
                posNow=posNow+(long)tamanyo;
                afterPerBlockDown(tamanyo,pieceId,posNow);
                randomAccessFile.close();
                httpget.abort();
                onePieceComplete();
            }
            catch (Exception e)
            {
                Log.d("EnergyOTA",e.toString());
            }
        }

        public void setDownloadListener(es.energy.energyotaupdater.FileDownloadTask.DownloadListener downloadListener)
        {
            dListener=downloadListener;
        }

        public DownloadFilePiece(File dfile, int id, long start, long end, long actual, boolean flag)
        {
            super();
            file=dfile;
            startPosition=start;
            endPosition=end;
            isRange=flag;
            pieceId=id;
            posNow=actual;
        }
    }

    private static interface DownloadListener
    {
        public abstract void onErrorOcurre(int i, long l);

        public abstract void onPerBlockDown(int i, int j, long l);

        public abstract void onPieceComplete();
    }

    private static final int BUFF_SIZE=4096;
    public static final int ERR_CONNECT_TIMEOUT=1;
    public static final int ERR_FILELENGTH_NOMATCH=2;
    public static final int ERR_NOERR=0;
    public static final int ERR_NOT_EXISTS=4;
    public static final int ERR_REQUEST_STOP=3;
    public static final int PROGRESS_DOWNLOAD_COMPLETE=4;
    public static final int PROGRESS_START_COMPLETE=3;
    public static final int PROGRESS_STOP_COMPLETE=2;
    public static final int PROGRESS_UPDATE=1;
    private String TAG;
    private volatile int err;
    private boolean acceptRanges;
    private long contentLength;
    private boolean debug;
    private ExecutorService downloadThreadPool;
    private String fileName;
    private HttpClient httpClient;
    private String path;
    private int poolThreadNum;
    private Handler progressHandler;
    private long receivedCount;
    private String tempFileName;
    private URI downloadUri;
    private boolean requestStop;
    private Object sync;

    public FileDownloadTask(HttpClient httpclient, URI uri, String sdpath, String filename, int i)
    {
        TAG="FileDownloadTask";
        debug=false;
        acceptRanges=false;
        err=0;
        requestStop=false;
        sync=new Object();
        httpClient=httpclient;
        path=sdpath;
        downloadUri=uri;
        poolThreadNum=i;
        receivedCount=0L;
        if(filename==null)
        {
            String filename2=uri.toString();
            int inicio=1+filename2.lastIndexOf("/");
            int fin;
            if(filename2.lastIndexOf("?")>0)
            {
                fin=filename2.lastIndexOf("?");
            }
            else
            {
                fin=filename2.length();
            }
            fileName=filename2.substring(inicio,fin);
        }
        else
        {
            fileName=filename;
        }

    }

}
