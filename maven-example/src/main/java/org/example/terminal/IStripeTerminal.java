package org.example.terminal;

import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.external.models.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IStripeTerminal extends TerminalStatus {

    List<? extends CollectInputsResult> collectInputs(
            @NotNull CollectInputsParameters parameters
    ) throws Throwable;

    @NotNull Terminal getTerminal();
}
