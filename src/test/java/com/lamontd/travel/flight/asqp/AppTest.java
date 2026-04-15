package com.lamontd.travel.flight.asqp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    @Test
    void testMainWithNoArgs() {
        assertDoesNotThrow(() -> App.main(new String[]{}));
    }
}
