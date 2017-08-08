package org.witness.proofmode.service;

/**
 * Created by n8fr8 on 3/3/17.
 */
import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Example stub job to monitor when there is a change to photos in the media provider.
 */

@TargetApi(24)
public class VideosContentJob extends JobService {

    public static int CONTENT_JOB_ID = 10002;

    // The root URI of the media provider, to monitor for generic changes to its content.
    static final Uri MEDIA_URI = Uri.parse("content://" + MediaStore.AUTHORITY + "/");

    // Path segments for image-specific URIs in the provider.
    static final List<String> EXTERNAL_PATH_SEGMENTS
            = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.getPathSegments();

    // The columns we want to retrieve about a particular image.
    static final String[] PROJECTION = new String[] {
            MediaStore.Video.VideoColumns._ID, MediaStore.Video.VideoColumns.DATA
    };
    static final int PROJECTION_ID = 0;
    static final int PROJECTION_DATA = 1;

    // This is the external storage directory where cameras place pictures.
    static final String DCIM_DIR = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM).getPath();

    // A pre-built JobInfo we use for scheduling our job.
    static final JobInfo JOB_INFO;

    static {
        JobInfo.Builder builder = new JobInfo.Builder(CONTENT_JOB_ID,
                new ComponentName("org.witness.proofmode", VideosContentJob.class.getName()));
        // Look for specific changes to images in the provider.
        builder.addTriggerContentUri(new JobInfo.TriggerContentUri(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,

                JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));


        // Also look for general reports of changes in the overall provider.
        builder.addTriggerContentUri(new JobInfo.TriggerContentUri(MEDIA_URI, 0));
        builder.setTriggerContentMaxDelay(3000);
        JOB_INFO = builder.build();
    }

    // Fake job work.  A real implementation would do some work on a separate thread.
    final Handler mHandler = new Handler();
    final Runnable mWorker = new Runnable() {
        @Override public void run() {
            scheduleJob(VideosContentJob.this);
            jobFinished(mRunningParams, false);
        }
    };

    JobParameters mRunningParams;

    // Schedule this job, replace any existing one.
    public static void scheduleJob(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        js.schedule(JOB_INFO);
        Log.i("VideosContentJob", "JOB SCHEDULED!");
    }

    // Check whether this job is currently scheduled.
    public static boolean isScheduled(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        List<JobInfo> jobs = js.getAllPendingJobs();
        if (jobs == null) {
            return false;
        }
        for (int i=0; i<jobs.size(); i++) {
            if (jobs.get(i).getId() == CONTENT_JOB_ID) {
                return true;
            }
        }
        return false;
    }

    // Cancel this job, if currently scheduled.
    public static void cancelJob(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        js.cancel(CONTENT_JOB_ID);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
//        Log.i("PhotosContentJob", "JOB STARTED!");
        mRunningParams = params;

        // Did we trigger due to a content change?
        if (params.getTriggeredContentAuthorities() != null) {
            boolean rescanNeeded = false;

            if (params.getTriggeredContentUris() != null) {
                // If we have details about which URIs changed, then iterate through them
                // and collect either the ids that were impacted or note that a generic
                // change has happened.
                ArrayList<String> ids = new ArrayList<>();
                for (Uri uri : params.getTriggeredContentUris()) {
                    List<String> path = uri.getPathSegments();
                    if (path != null && path.size() == EXTERNAL_PATH_SEGMENTS.size()+1) {
                        // This is a specific file.
                        ids.add(path.get(path.size()-1));
                    } else {
                        // Oops, there is some general change!
                        rescanNeeded = true;
                    }
                }

                if (ids.size() > 0) {
                    // If we found some ids that changed, we want to determine what they are.
                    // First, we do a query with content provider to ask about all of them.
                    StringBuilder selection = new StringBuilder();
                    for (int i=0; i<ids.size(); i++) {
                        if (selection.length() > 0) {
                            selection.append(" OR ");
                        }
                        selection.append(MediaStore.Video.VideoColumns._ID);
                        selection.append("='");
                        selection.append(ids.get(i));
                        selection.append("'");
                    }

                    // Now we iterate through the query, looking at the filenames of
                    // the items to determine if they are ones we are interested in.
                    Cursor cursor = null;
                    boolean haveFiles = false;
                    try {
                        cursor = getContentResolver().query(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                PROJECTION, selection.toString(), null, null);
                        while (cursor.moveToNext()) {

                            // We only care about files in the DCIM directory.
                            String path = cursor.getString(PROJECTION_DATA);
                            if (path.startsWith(DCIM_DIR)) {

                                //NEW PHOTOS FOUND!
                                haveFiles = true;

                                Intent intent = new Intent();
                                intent.setData(Uri.fromFile(new File(path)));
                                new MediaWatcher().onReceive(VideosContentJob.this,intent);

                            }
                        }
                    } catch (SecurityException e) {
                        //sb.append("Error: no access to media!");

                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }

            } else {
                // We don't have any details about URIs (because too many changed at once),
                // so just note that we need to do a full rescan.
                rescanNeeded = true;
            }

        } else {

        }

        // We will emulate taking some time to do this work, so we can see batching happen.
        mHandler.postDelayed(mWorker, 10*1000);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        mHandler.removeCallbacks(mWorker);
        return false;
    }
}