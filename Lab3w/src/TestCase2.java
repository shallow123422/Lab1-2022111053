import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.*;

import static org.junit.Assert.*;

public class TestCase2 {
    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream testOut;

    @Before
    public void setUpStreams() {
        testOut = new ByteArrayOutputStream();
        // 双重输出：控制台实时显示 + 内容捕获
        System.setOut(new PrintStream(new TeeOutputStream(originalOut, testOut)));
    }

    @After
    public void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    // 自定义双写输出流
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
    public void testCase2_noFirstWordInGraph() throws Exception {
        // 模拟完整交互流程
        String input = String.join(System.lineSeparator(),
                "Cursed Be The Treasure.txt",  // 文件路径输入
                "1",                           // 选择功能1（查找桥接词）
                "123 of",                      // 测试输入（第一个词不存在）
                "6",                           // 退出选项
                ""                             // 确认退出
        );
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        // 执行主程序（输出会同时显示在控制台）
        Main.main(new String[]{});

        // 获取并标准化输出
        String fullOutput = testOut.toString()
                .replaceAll("\r\n", "\n")      // 统一换行符
                .replaceAll("\\s+", " ");      // 合并连续空白

        // 带诊断信息的断言（支持不同提示变体）
        String expectedPattern1 = "No 123 in the graph!";

        assertTrue("实际输出内容：\n" + testOut.toString() + "\n未找到预期错误提示\n" +
                        "预期应包含：" + expectedPattern1 + "\n或：",
                fullOutput.contains(expectedPattern1));
    }
}
