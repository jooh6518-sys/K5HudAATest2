package com.joohkim.k5hudtest;

import java.lang.ref.WeakReference;

final class HudCommandBus {
    interface Listener {
        void onCommand(Command command);
    }

    interface ResultListener {
        void onResult(String result);
    }

    static final class Command {
        final boolean end;
        final int maneuverType;
        final int meters;
        final String road;
        final String label;

        private Command(boolean end, int maneuverType, int meters, String road, String label) {
            this.end = end;
            this.maneuverType = maneuverType;
            this.meters = meters;
            this.road = road;
            this.label = label;
        }

        static Command trip(int maneuverType, int meters, String road, String label) {
            return new Command(false, maneuverType, meters, road, label);
        }

        static Command end() {
            return new Command(true, 0, 0, "", "안내 종료");
        }
    }

    private static WeakReference<Listener> listenerRef = new WeakReference<>(null);
    private static WeakReference<ResultListener> resultRef = new WeakReference<>(null);
    private static Command pending;
    private static String lastResult = "대기 중";

    static synchronized void register(Listener listener) {
        listenerRef = new WeakReference<>(listener);
        report("Android Auto 세션 연결됨");
        if (pending != null) {
            Command c = pending;
            pending = null;
            listener.onCommand(c);
        }
    }

    static synchronized void unregister(Listener listener) {
        Listener current = listenerRef.get();
        if (current == listener) {
            listenerRef.clear();
            report("Android Auto 세션 연결 해제됨");
        }
    }

    static synchronized boolean dispatch(Command command) {
        Listener listener = listenerRef.get();
        if (listener != null) {
            listener.onCommand(command);
            return true;
        }
        pending = command;
        return false;
    }

    static synchronized void setResultListener(ResultListener listener) {
        resultRef = new WeakReference<>(listener);
        listener.onResult(lastResult);
    }

    static synchronized void clearResultListener(ResultListener listener) {
        ResultListener current = resultRef.get();
        if (current == listener) resultRef.clear();
    }

    static synchronized void report(String result) {
        lastResult = result;
        ResultListener listener = resultRef.get();
        if (listener != null) listener.onResult(result);
    }

    static synchronized String getLastResult() {
        return lastResult;
    }

    private HudCommandBus() {}
}
