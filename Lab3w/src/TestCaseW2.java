import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.*;

import static org.junit.Assert.*;

public class TestCaseW2 {
    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream testOut;

    @Before
    public void setUpStreams() {
        testOut = new ByteArrayOutputStream();
        // 双重输出：既保留控制台输出，又捕获输出内容
        System.setOut(new PrintStream(new TeeOutputStream(originalOut, new PrintStream(testOut))));
    }

    @After
    public void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    // 自定义输出流实现双写
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
        // 模拟三步输入（文件名、菜单选项、测试输入）
        String input = String.join(System.lineSeparator(),
                "Easy test.txt",
                "3",
                "again the"  // 单次测试输入
        );
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        // 执行主程序（此时输出会同时打印到控制台和testOut）
        Main.main(new String[]{});

        // 获取标准化后的输出内容
        String normalizedOutput = testOut.toString()
                .replaceAll("\r\n", "\n")  // 统一换行符
                .replaceAll("\\s+", " ");  // 合并连续空白

        // 带详细错误信息的断言
        assertTrue("实际输出内容：\n" + testOut.toString() + "\n未找到预期错误信息",
                normalizedOutput.contains("No path from again to the!"));
    }
}
