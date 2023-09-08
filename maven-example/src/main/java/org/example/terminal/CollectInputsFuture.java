package org.example.terminal;

import com.stripe.stripeterminal.external.callable.CollectInputsResultCallback;
import com.stripe.stripeterminal.external.models.CollectInputsResult;
import com.stripe.stripeterminal.external.models.TerminalException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CollectInputsFuture extends CompletableFuture<List<? extends CollectInputsResult>>
    implements CollectInputsResultCallback {
  @Override
  public void onSuccess(@NotNull List<? extends CollectInputsResult> list) {
    complete(list);
  }

  @Override
  public void onFailure(@NotNull TerminalException e) {
    completeExceptionally(e);
  }
}
