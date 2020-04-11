package me.saket.dank.save;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.text.format.DateUtils;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Identifiable;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.DankJobService;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.compose.SimpleIdentifiable;
import me.saket.dank.vote.VotingManager;
import timber.log.Timber;

/**
 * Used for re-trying failed vote attempts.
 */
public class SaveJobService extends DankJobService {

  private static final String KEY_SAVEABLE_JSON = "votableJson";
  private static final String KEY_SAVED = "voteDirection";

  @Inject Moshi moshi;
  @Inject SaveManager saveManager;

  /**
   * Schedule a voting attempt whenever JobScheduler deems it fit.
   */
  public static void scheduleRetry(Context context, Identifiable saveableContribution, boolean saved, Moshi moshi) {
    PersistableBundle extras = new PersistableBundle(2);
    extras.putString(KEY_SAVED, saved + "");

    JsonAdapter<SimpleIdentifiable> adapter = moshi.adapter(SimpleIdentifiable.class);
    String votableJson = adapter.toJson(SimpleIdentifiable.Companion.from(saveableContribution));
    extras.putString(KEY_SAVEABLE_JSON, votableJson);

    JobInfo retryJobInfo = new JobInfo.Builder(ID_VOTE + saveableContribution.hashCode(), new ComponentName(context, SaveJobService.class))
        .setMinimumLatency(5 * DateUtils.MINUTE_IN_MILLIS)
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .setExtras(extras)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.schedule(retryJobInfo);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Dank.dependencyInjector().inject(this);
  }

  @Override
  public JobStartCallback onStartJob2(JobParameters params) {
    //noinspection ConstantConditions
    boolean saved = Boolean.parseBoolean(params.getExtras().getString(KEY_SAVED));

    JsonAdapter<SimpleIdentifiable> jsonAdapter = moshi.adapter(SimpleIdentifiable.class);
    String votableJson = params.getExtras().getString(KEY_SAVEABLE_JSON);

    //noinspection ConstantConditions
    Single.fromCallable(() -> jsonAdapter.fromJson(votableJson))
        .flatMapCompletable(saveableContribution -> {
          if (!saveManager.isSavePending(saveableContribution)) {
            // Looks like the pending vote was cleared upon refreshing data from remote.
            Timber.w("Job is stale because contribution no longer has a pending vote: %s", saveableContribution);
            return Completable.complete();

          } else {
            return saveManager.saveAndSend(Save.create(saveableContribution, saved));
          }
        })
        .ambWith(lifecycleOnDestroy().ignoreElements())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            () -> {
              Timber.i("Finis");
              jobFinished(params, false);
            },
            error -> {
              boolean needsReschedule;

              if (VotingManager.isTooManyRequestsError(error)) {
                Timber.i("Received 429-too-many-requests. Will retry vote later.");
                needsReschedule = true;

              } else {
                ResolvedError resolvedError = Dank.errors().resolve(error);
                needsReschedule = resolvedError.isNetworkError() || resolvedError.isRedditServerError() || resolvedError.isUnknown();
                Timber.i("Retry failed: %s", error.getMessage());
              }

              Timber.i("needsReschedule: %s", needsReschedule);
              jobFinished(params, needsReschedule);
            }
        );

    return JobStartCallback.runningInBackground();
  }

  @Override
  public JobStopCallback onStopJob2() {
    return JobStopCallback.rescheduleRequired();
  }
}
