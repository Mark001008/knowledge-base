package com.ma.kb.core.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextCleanerTest {

    private TextCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner = new TextCleaner();
    }

    @Test
    void cleanNormalText() {
        String input = "  Hello   World  ";
        String result = cleaner.clean(input);
        assertEquals("Hello World", result);
    }

    @Test
    void cleanMultipleBlankLines() {
        String input = "Line1\n\n\n\nLine2";
        String result = cleaner.clean(input);
        assertEquals("Line1\n\nLine2", result);
    }

    @Test
    void cleanWindowsLineEndings() {
        String input = "Line1\r\nLine2\rLine3";
        String result = cleaner.clean(input);
        assertEquals("Line1\nLine2\nLine3", result);
    }

    @Test
    void cleanNull() {
        assertEquals("", cleaner.clean(null));
    }

    @Test
    void cleanBlank() {
        assertEquals("", cleaner.clean("   "));
    }

    @Test
    void cleanChineseText() {
        String input = "  你好   世界  \n\n\n\n  测试  ";
        String result = cleaner.clean(input);
        assertEquals("你好 世界\n\n测试", result);
    }
}
