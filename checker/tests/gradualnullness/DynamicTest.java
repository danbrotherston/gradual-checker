package examples;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.Dynamic;

public class DynamicTest {
    
    void a() {
        @Dynamic String f = "abc";
        f = null;

        @Nullable String h = g();
    }

    private @Dynamic String g() {
      return "123";
    }
}
