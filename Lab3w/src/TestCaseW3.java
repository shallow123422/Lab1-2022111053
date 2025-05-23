import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public class TestCaseW3 {
    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream testOut;

    @Before
    public void setUpStreams() {
        testOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(new TeeOutputStream(originalOut, new PrintStream(testOut))));
    }

    @After
    public void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    // 自定义输出流，实现双向输出
    private static class TeeOutputStream extends OutputStream {
        private final OutputStream out1;
        private final OutputStream out2;

        public TeeOutputStream(OutputStream out1, OutputStream out2) {
            this.out1 = out1;
            this.out2 = out2;
        }

        @Override
        public void write(int b) throws IOException {
            out1.write(b);
            out2.write(b);
        }
    }

    @Test
    public void testCase5_invalidInputLength() throws Exception {
        String input = String.join(System.lineSeparator(),
                "Easy test.txt",  // 输入文件名
                "3",              // 菜单选项：Shortest Path
                "and again"       // 起点和终点
        );
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Main.main(new String[]{});

        String output = testOut.toString();

        // 提供更宽松的多段匹配
        assertTrue("输出中未找到路径提示信息:\n" + output,
                output.contains("Shortest Path Highlighted:"));

        assertTrue("输出中未找到预期路径:\n" + output,
                output.contains("and -> shared -> the -> scientist -> analyzed -> it -> again"));

        assertTrue("输出中未找到路径总权重:\n" + output,
                output.contains("Total path weight: 7"));
    }
}
