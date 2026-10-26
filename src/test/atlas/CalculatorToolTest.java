package atlas;

import atlas.examples.tools.CalculatorTool;
import atlas.protocol.Content;
import atlas.protocol.Tool;
import atlas.protocol.ToolException;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculatorToolTest {

    private final Tool tool = CalculatorTool.create();

    private String call(String expression) throws ToolException {
        List<Content> c = tool.call(Map.of("expression", expression));
        return c.get(0).getText();
    }

    @Test
    void basicArithmetic() throws ToolException {
        assertEquals("2+3*4 = 14", call("2+3*4"));
        assertEquals("(2+3)*4 = 20", call("(2+3)*4"));
    }

    @Test
    void precedenceAndParens() throws ToolException {
        assertEquals("(2+3)*4/2 = 10", call("(2+3)*4/2"));
    }

    @Test
    void unaryMinusAndModulo() throws ToolException {
        assertEquals("-5+8 = 3", call("-5+8"));
        assertEquals("10%3 = 1", call("10%3"));
    }

    @Test
    void divisionByZeroIsError() {
        ToolException ex = assertThrows(ToolException.class, () -> call("10/0"));
        assertTrue(ex.getMessage().toLowerCase().contains("zero"));
    }

    @Test
    void invalidExpressionIsError() {
        assertThrows(ToolException.class, () -> call("2++"));
    }

    @Test
    void missingArgumentIsError() {
        assertThrows(ToolException.class, () -> tool.call(Map.of()));
    }
}
