package me.saket.dank.save;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Identifiable;
import net.dean.jraw.models.Submission;

import io.reactivex.Completable;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.walkthrough.SyntheticData;
import timber.log.Timber;

public interface Save {

    Identifiable contributionToVote();

    boolean saved();

    Completable saveAndSend(SaveManager saveManager);

    static Save create(Identifiable contributionToVote, boolean saved) {
        boolean isNoOpVote;

        if (contributionToVote instanceof Submission) {
            String submissionId = contributionToVote.getId();
            isNoOpVote = submissionId.equalsIgnoreCase(SyntheticData.SUBMISSION_ID_FOR_GESTURE_WALKTHROUGH);
        } else {
            isNoOpVote = true;
        }
        if (!isNoOpVote) {
            return new AutoValue_Save_RealSave(contributionToVote, saved);
        }
        return new AutoValue_Save_NoOpSave(contributionToVote, saved);
    }

    Completable sendToRemote(Reddit reddit);

    @AutoValue
    abstract class RealSave implements Save {

        @Override
        public Completable saveAndSend(SaveManager saveManager) {
            return saveManager.voteWithAutoRetry(this);
        }

        @Override
        public Completable sendToRemote(Reddit reddit) {
            return reddit.loggedInUser().save(contributionToVote(), saved());
        }
    }

    @AutoValue
    abstract class NoOpSave implements Save {

        @Override
        public Completable saveAndSend(SaveManager saveManager) {
            return saveManager.voteWithAutoRetry(this);
        }

        @Override
        public Completable sendToRemote(Reddit reddit) {
            Timber.i("Ignoring save in synthetic-submission-for-gesture-walkthrough");
            return Completable.complete();
        }
    }
}
