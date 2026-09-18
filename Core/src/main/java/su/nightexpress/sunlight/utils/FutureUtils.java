package su.nightexpress.sunlight.utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class FutureUtils {

    public static <T> CompletableFuture<T> loggable(CompletableFuture<T> future) {
        return future.whenComplete(FutureUtils::printStacktrace);
    }

    public static <T> void printStacktrace(T object, Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
