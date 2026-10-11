package de.robv.android.xposed;

import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Member;

public abstract class XC_MethodHook {
    public final int priority;

    public XC_MethodHook() {
        this.priority = 50;
    }

    public XC_MethodHook(int priority) {
        this.priority = priority;
    }

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    public static class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;
        private Object result = null;
        private Throwable throwable = null;
        public boolean returnEarly = false;

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
            this.throwable = null;
            this.returnEarly = true;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
            this.result = null;
            this.returnEarly = true;
        }

        public Object getResultOrThrowable() throws Throwable {
            if (throwable != null) throw throwable;
            return result;
        }
    }

    public static class Unhook {
        private final XposedInterface.HookHandle handle;

        public Unhook(XposedInterface.HookHandle handle) {
            this.handle = handle;
        }

        public void unhook() {
            // Unhook handle
        }
    }

    public static class LibXposedHookerBridge implements XposedInterface.Hooker {
        private final XC_MethodHook hook;

        public LibXposedHookerBridge(XC_MethodHook hook) {
            this.hook = hook;
        }

        @Override
        public Object intercept(XposedInterface.Chain chain) throws Throwable {
            MethodHookParam param = new MethodHookParam();
            param.method = chain.getExecutable();
            param.thisObject = chain.getThisObject();
            param.args = chain.getArgs().toArray();

            try {
                hook.beforeHookedMethod(param);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            if (param.returnEarly) {
                if (param.throwable != null) throw param.throwable;
                return param.result;
            }

            try {
                param.result = chain.proceed(param.args);
            } catch (Throwable t) {
                param.throwable = t;
            }

            try {
                hook.afterHookedMethod(param);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            if (param.throwable != null) throw param.throwable;
            return param.result;
        }
    }
}
