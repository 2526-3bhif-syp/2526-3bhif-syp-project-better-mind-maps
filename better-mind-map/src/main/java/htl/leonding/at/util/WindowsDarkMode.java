package htl.leonding.at.util;
import htl.leonding.at.model.*;
import htl.leonding.at.controller.*;
import htl.leonding.at.service.*;
import htl.leonding.at.repository.*;
import htl.leonding.at.util.*;
import htl.leonding.at.App;


import java.lang.foreign.*;
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Sets the Windows title bar to dark mode via DwmSetWindowAttribute.
 * Uses the Glass windowing layer via reflection and the FFM API (Java 21+).
 * Falls back silently on non-Windows systems or unsupported JVM configurations.
 */
public class WindowsDarkMode {

    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static volatile boolean supported =
            System.getProperty("os.name", "").toLowerCase().startsWith("windows");

    public static void applyToAllWindows() {
        if (!supported) return;
        try {
            Class<?> winClass = Class.forName("com.sun.glass.ui.Window");
            Method getWindows = winClass.getDeclaredMethod("getWindows");
            getWindows.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> windows = (List<Object>) getWindows.invoke(null);
            Method getNativeHandle = winClass.getDeclaredMethod("getNativeHandle");
            getNativeHandle.setAccessible(true);
            for (Object w : windows) {
                long hwnd = (long) getNativeHandle.invoke(w);
                if (hwnd != 0) applyDark(hwnd);
            }
        } catch (Throwable t) {
            supported = false;
        }
    }

    @SuppressWarnings("restricted")
    private static void applyDark(long hwnd) {
        try (Arena arena = Arena.ofConfined()) {
            SymbolLookup lib = SymbolLookup.libraryLookup("dwmapi", arena);
            MemorySegment fn = lib.find("DwmSetWindowAttribute").orElseThrow();
            MethodHandle mh = Linker.nativeLinker().downcallHandle(fn,
                    FunctionDescriptor.of(ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT));
            MemorySegment value = arena.allocate(ValueLayout.JAVA_INT, 1);
            mh.invoke(MemorySegment.ofAddress(hwnd),
                    DWMWA_USE_IMMERSIVE_DARK_MODE, value, 4);
        } catch (Throwable ignored) {
            supported = false;
        }
    }
}
