package ch.lumarlie.railer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class XYZTest {

    @Test
    void should_detect_turn() {
        Assertions.assertTrue(XYZ.isTurn(new XYZ(1, 2, 5), new XYZ(1, 2, 6), new XYZ(2, 2, 6)));
        Assertions.assertTrue(XYZ.isTurn(new XYZ(1, 2, 5), new XYZ(2, 2, 2), new XYZ(2, 2, 6)));
        Assertions.assertFalse(XYZ.isTurn(new XYZ(1, 2, 5), new XYZ(1, 2, 6), new XYZ(1, 2, 7)));
    }

    @Test
    void should_detect_step_turn() {
        Assertions.assertTrue(XYZ.isStepTurn(new XYZ(1, 2, 5), new XYZ(1, 2, 6), new XYZ(2, 1, 6)));
        Assertions.assertTrue(XYZ.isStepTurn(new XYZ(1, 2, 5), new XYZ(2, 2, 2), new XYZ(2, 1, 6)));

        Assertions.assertTrue(XYZ.isStepTurn(new XYZ(1, 1, 5), new XYZ(1, 2, 6), new XYZ(2, 1, 6)));
        Assertions.assertTrue(XYZ.isStepTurn(new XYZ(1, 1, 5), new XYZ(2, 2, 2), new XYZ(2, 1, 6)));

        Assertions.assertTrue(XYZ.isStepTurn(new XYZ(1, 1, 5), new XYZ(1, 2, 6), new XYZ(2, 2, 6)));
        Assertions.assertTrue(XYZ.isStepTurn(new XYZ(1, 1, 5), new XYZ(2, 2, 2), new XYZ(2, 2, 6)));

        Assertions.assertTrue(XYZ.isStepTurn(new XYZ(1, 1, 5), new XYZ(1, 2, 6), new XYZ(2, 3, 6)));
        Assertions.assertTrue(XYZ.isStepTurn(new XYZ(1, 1, 5), new XYZ(2, 2, 2), new XYZ(2, 3, 6)));

        Assertions.assertFalse(XYZ.isStepTurn(new XYZ(1, 2, 5), new XYZ(1, 2, 6), new XYZ(2, 2, 6)));
        Assertions.assertFalse(XYZ.isStepTurn(new XYZ(1, 2, 5), new XYZ(2, 2, 2), new XYZ(2, 2, 6)));
    }

}