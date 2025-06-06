// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package externalApp.nativessh;


import externalApp.ExternalApp;
import externalApp.ExternalAppEntry;
import externalApp.ExternalAppUtil;
import externalApp.ExternalCli;

/**
 * <p>This is a program that would be called by ssh when key passphrase is needed,
 * and if {@code SSH_ASKPASS} variable is set to the script that invokes this program.</p>
 * <p>ssh expects the reply from the program's standard output.</p>
 */
public class NativeSshAskPassApp implements ExternalApp, ExternalCli {

  public static void main(String[] args) {
    var exitCode = new NativeSshAskPassApp().entryPoint(ExternalAppEntry.fromMain(args));
    System.exit(exitCode);
  }

  @Override
  public int entryPoint(ExternalAppEntry entry) {
    return ExternalAppUtil.handleAskPassInvocation(NativeSshAskPassAppHandler.IJ_SSH_ASK_PASS_HANDLER_ENV,
                                                   NativeSshAskPassAppHandler.IJ_SSH_ASK_PASS_PORT_ENV,
                                                   NativeSshAskPassAppHandler.ENTRY_POINT_NAME,
                                                   entry);
  }
}
