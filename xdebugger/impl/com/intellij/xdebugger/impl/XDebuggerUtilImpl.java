package com.intellij.xdebugger.impl;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * @author nik
 */
public class XDebuggerUtilImpl extends XDebuggerUtil {
  private XLineBreakpointType<?>[] myLineBreakpointTypes;
  private Map<Class<? extends XBreakpointType<?,?>>, XBreakpointType<?,?>> myTypeByClass;

  public XLineBreakpointType<?>[] getLineBreakpointTypes() {
    if (myLineBreakpointTypes == null) {
      XBreakpointType[] types = XBreakpointType.getBreakpointTypes();
      List<XLineBreakpointType<?>> lineBreakpointTypes = new ArrayList<XLineBreakpointType<?>>();
      for (XBreakpointType type : types) {
        if (type instanceof XLineBreakpointType<?>) {
          lineBreakpointTypes.add((XLineBreakpointType<?>)type);
        }
      }
      myLineBreakpointTypes = lineBreakpointTypes.toArray(new XLineBreakpointType<?>[lineBreakpointTypes.size()]);
    }
    return myLineBreakpointTypes;
  }

  public void toggleLineBreakpoint(@NotNull final Project project, @NotNull final VirtualFile file, final int line) {
    for (XLineBreakpointType<?> type : getLineBreakpointTypes()) {
      if (type.canPutAt(file, line)) {
        toggleLineBreakpoint(project, type, file, line);
        return;
      }
    }
  }

  public <P extends XBreakpointProperties> void toggleLineBreakpoint(@NotNull final Project project, @NotNull final XLineBreakpointType<P> type, @NotNull final VirtualFile file,
                                                                     final int line) {
    new WriteAction() {
      protected void run(final Result result) {
        XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
        XLineBreakpoint<P> breakpoint = breakpointManager.findBreakpointAtLine(type, file, line);
        if (breakpoint != null) {
          breakpointManager.removeBreakpoint(breakpoint);
        }
        else {
          P properties = type.createBreakpointProperties(file, line);
          breakpointManager.addLineBreakpoint(type, file.getUrl(), line, properties);
        }
      }
    }.execute();
  }

  public void removeBreakpoint(final Project project, final XBreakpoint<?> breakpoint) {
    new WriteAction() {
      protected void run(final Result result) {
        XDebuggerManager.getInstance(project).getBreakpointManager().removeBreakpoint(breakpoint);
      }
    }.execute();
  }

  public <B extends XBreakpoint<?>> XBreakpointType<B, ?> findBreakpointType(@NotNull Class<? extends XBreakpointType<B, ?>> typeClass) {
    if (myTypeByClass == null) {
      myTypeByClass = new HashMap<Class<? extends XBreakpointType<?,?>>, XBreakpointType<?,?>>();
      for (XBreakpointType<?, ?> breakpointType : XBreakpointType.getBreakpointTypes()) {
        if (breakpointType.getClass().equals(typeClass)) {
          myTypeByClass.put(typeClass, breakpointType);
        }
      }
    }
    XBreakpointType<?, ?> type = myTypeByClass.get(typeClass);
    //noinspection unchecked
    return (XBreakpointType<B, ?>)type;
  }
}
