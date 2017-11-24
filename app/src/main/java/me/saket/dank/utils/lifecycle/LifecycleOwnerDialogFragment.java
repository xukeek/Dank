package me.saket.dank.utils.lifecycle;

import android.content.Intent;
import android.os.Bundle;

import com.trello.navi2.component.support.NaviDialogFragment;

public class LifecycleOwnerDialogFragment extends NaviDialogFragment implements LifecycleOwner {

  // Can do without FragmentLifecycleStreams for now.
  private ActivityLifecycleStreams lifecycleStreams;

  @Override
  public LifecycleStreams lifecycle() {
    return lifecycleStreams;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    lifecycleStreams = new ActivityLifecycleStreams();
    lifecycleStreams.notifyOnCreate();
  }

  @Override
  public void onStart() {
    super.onStart();
    lifecycleStreams.notifyOnStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    lifecycleStreams.notifyOnResume();
  }

  @Override
  public void onPause() {
    lifecycleStreams.notifyOnPause();
    super.onPause();
  }

  @Override
  public void onStop() {
    lifecycleStreams.notifyOnStop();
    super.onStop();
  }

  @Override
  public void onDestroy() {
    lifecycleStreams.notifyOnDestroy();
    super.onDestroy();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    lifecycleStreams.notifyOnActivityResult(requestCode, resultCode, data);
  }
}