package org.example;

import com.stripe.stripeterminal.Terminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;


class MainTest {
    @Test()
    public void getTerminalBeforeIntFailsWithIllegalStateException() {
        assertThrows(IllegalStateException.class, Terminal::getInstance);
    }
}