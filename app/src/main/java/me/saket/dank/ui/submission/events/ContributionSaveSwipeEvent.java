package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.PublicContribution;

import me.saket.dank.data.SwipeEvent;
import me.saket.dank.save.Save;

@AutoValue
public abstract class ContributionSaveSwipeEvent implements SwipeEvent {

  public abstract PublicContribution contribution();

  public abstract boolean saved();

  public Save toSave() {
    return Save.create(contribution(), saved());
  }

  public static ContributionSaveSwipeEvent create(PublicContribution contribution, boolean saved) {
    return new AutoValue_ContributionSaveSwipeEvent(contribution, saved);
  }
}
