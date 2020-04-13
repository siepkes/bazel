// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.runtime;

import com.google.common.base.Preconditions;
import com.google.devtools.build.lib.unix.UnixFileSystem;
import com.google.devtools.build.lib.util.AbruptExitException;
import com.google.devtools.build.lib.util.ExitCode;
import com.google.devtools.build.lib.util.OS;
import com.google.devtools.build.lib.vfs.DigestHashFunction;
import com.google.devtools.build.lib.vfs.DigestHashFunction.DefaultAlreadySetException;
import com.google.devtools.build.lib.vfs.DigestHashFunction.DefaultHashFunctionNotSetException;
import com.google.devtools.build.lib.vfs.DigestHashFunction.DigestFunctionConverter;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.JavaIoFileSystem;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.windows.WindowsFileSystem;
import com.google.devtools.common.options.OptionsParsingException;
import com.google.devtools.common.options.OptionsParsingResult;

/**
 * Module to provide a {@link FileSystem} instance that uses {@code SHA256} as the default hash
 * function, or else what's specified by {@code -Dbazel.DigestFunction}.
 *
 * <p>For legacy reasons we can't make the {@link FileSystem} class use {@code SHA256} by default.
 */
public class BazelFileSystemModule extends BlazeModule {

  @Override
  public void globalInit(OptionsParsingResult startupOptionsProvider) throws AbruptExitException {
    BlazeServerStartupOptions startupOptions =
        Preconditions.checkNotNull(
            startupOptionsProvider.getOptions(BlazeServerStartupOptions.class));
    DigestHashFunction commandLineHashFunction = startupOptions.digestHashFunction;
    try {
      if (commandLineHashFunction != null) {
        DigestHashFunction.setDefault(commandLineHashFunction);
      } else {
        String value = System.getProperty("bazel.DigestFunction", "SHA256");
        DigestHashFunction jvmPropertyHash;
        try {
          jvmPropertyHash = new DigestFunctionConverter().convert(value);
        } catch (OptionsParsingException e) {
          throw new AbruptExitException(ExitCode.COMMAND_LINE_ERROR, e);
        }
        DigestHashFunction.setDefault(jvmPropertyHash);
      }
    } catch (DefaultAlreadySetException e) {
      throw new AbruptExitException(ExitCode.BLAZE_INTERNAL_ERROR, e);
    }
  }

  @Override
  public ModuleFileSystem getFileSystem(
      OptionsParsingResult startupOptions, PathFragment realExecRootBase)
      throws DefaultHashFunctionNotSetException {
//    if (OS.getCurrent() == OS.ILLUMOS) {
      // TODO: Temporary Illumos workarround. The 'Java_com_google_devtools_build_lib_unix_NativePosixFiles_mkdirs'
      // method in unix_jni.cc throws an exception with 'Error 0'. See stack trace below. Obtained from jvm.log with
      // a modification which made Bazel print the Java exception.
      //
//      java.io.IOException: /root/.cache/bazel/_bazel_root/cache/repos/v1 (Error 0)
//      at com.google.devtools.build.lib.unix.NativePosixFiles.mkdirs(Native Method)
//      at com.google.devtools.build.lib.unix.UnixFileSystem.createDirectoryAndParents(UnixFileSystem.java:331)
//      at com.google.devtools.build.lib.vfs.Path.createDirectoryAndParents(Path.java:549)
//      at com.google.devtools.build.lib.vfs.FileSystemUtils.createDirectoryAndParents(FileSystemUtils.java:614)
//      at com.google.devtools.build.lib.bazel.BazelRepositoryModule.beforeCommand(BazelRepositoryModule.java:251)
//      at com.google.devtools.build.lib.runtime.BlazeCommandDispatcher.execExclusively(BlazeCommandDispatcher.java:358)
//      at com.google.devtools.build.lib.runtime.BlazeCommandDispatcher.exec(BlazeCommandDispatcher.java:208)
//      at com.google.devtools.build.lib.server.GrpcServerImpl.executeCommand(GrpcServerImpl.java:604)
//      at com.google.devtools.build.lib.server.GrpcServerImpl.lambda$run$2(GrpcServerImpl.java:660)
//      at io.grpc.Context$1.run(Context.java:595)
//      at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
//      at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
//      at java.lang.Thread.run(Thread.java:748)

      return ModuleFileSystem.create(new JavaIoFileSystem());
//    }
//    if ("0".equals(System.getProperty("io.bazel.EnableJni"))) {
      // Ignore UnixFileSystem, to be used for bootstrapping.
//      return ModuleFileSystem.create(
//          OS.getCurrent() == OS.WINDOWS ? new WindowsFileSystem() : new JavaIoFileSystem());
//    }
    // The JNI-based UnixFileSystem is faster, but on Windows it is not available.
//    return ModuleFileSystem.create(
//        OS.getCurrent() == OS.WINDOWS ? new WindowsFileSystem() : new UnixFileSystem());
  }
}
