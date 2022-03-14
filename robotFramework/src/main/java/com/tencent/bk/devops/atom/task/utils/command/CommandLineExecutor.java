package com.tencent.bk.devops.atom.task.utils.command;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CommandLineExecutor extends DefaultExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineExecutor.class);

    /**
     * the first exception being caught to be thrown to the caller
     */
    private IOException exceptionCaught = null;

    @Override
    public int execute(CommandLine command, Map<String, String> environment) throws IOException {
        if (getWorkingDirectory() != null && !getWorkingDirectory().exists()) {
            throw new IOException("$workingDirectory doesn't exist.");
        }

        return executeInternal(command, environment, getWorkingDirectory(), getStreamHandler());
    }

    /**
     * Execute an internal process. If the executing thread is interrupted while waiting for the
     * child process to return the child process will be killed.
     *
     * @param command     the command to execute
     * @param environment the execution environment
     * @param dir         the working directory
     * @param streams     process the streams (in, out, err) of the process
     * @return the exit code of the process
     * @throws IOException executing the process failed
     */
    private int executeInternal(CommandLine command, Map<String, String> environment, File dir, ExecuteStreamHandler streams) throws IOException {

        setExceptionCaught(null);

        Process process = this.launch(command, environment, dir);

        try {
            streams.setProcessInputStream(process.getOutputStream());
            streams.setProcessOutputStream(process.getInputStream());
            streams.setProcessErrorStream(process.getErrorStream());
        } catch (IOException e) {
            process.destroy();
            throw e;
        }

        streams.start();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {

            // add the process to the list of those to destroy if the VM exits
            if (this.getProcessDestroyer() != null) {
                this.getProcessDestroyer().add(process);
            }

            // associate the watchdog with the newly created process
            if (getWatchdog() != null) {
                getWatchdog().start(process);
            }

            int exitValue = Executor.INVALID_EXITVALUE;

            try {
                exitValue = process.waitFor();
            } catch (InterruptedException e) {
                process.destroy();
            } finally {
                Thread.interrupted();
            }

            if (getWatchdog() != null) {
                getWatchdog().stop();
            }

            try {
                Future future = executor.submit(() -> {
                    try {
                        streams.stop();
                    } catch (IOException e) {
                        setExceptionCaught(e);
                    }

                    closeProcessStreams(process);
                });
                // Wait 3 minute for stopping the stream
                future.get(3, TimeUnit.MINUTES);
            } catch (Throwable t) {
                logger.info("Fail to close the stream", t);
            }

            if (getExceptionCaught() != null) {
                throw getExceptionCaught();
            }

            if (getWatchdog() != null) {
                try {
                    getWatchdog().checkException();
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
            }

            if (this.isFailure(exitValue)) {
                throw new ExecuteException("Process exited with an error: " + exitValue, exitValue);
            }

            return exitValue;
        } finally {
            // remove the process to the list of those to destroy if the VM exits
            if (this.getProcessDestroyer() != null) {
                this.getProcessDestroyer().remove(process);
            }
            executor.shutdownNow();
        }
    }

    /**
     * Close the streams belonging to the given Process.
     *
     * @param process the <CODE>Process</CODE>.
     */
    private void closeProcessStreams(Process process) {

        try {
            process.getInputStream().close();
        } catch (IOException e) {
            setExceptionCaught(e);
        }

        try {
            process.getOutputStream().close();
        } catch (IOException e) {
            setExceptionCaught(e);
        }

        try {
            process.getErrorStream().close();
        } catch (IOException e) {
            setExceptionCaught(e);
        }
    }

    /**
     * Keep track of the first IOException being thrown.
     *
     * @param e the IOException
     */
    private void setExceptionCaught(IOException e) {
        if (this.exceptionCaught == null) {
            this.exceptionCaught = e;
        }
    }

    /**
     * Get the first IOException being thrown.
     *
     * @return the first IOException being caught
     */
    private IOException getExceptionCaught() {
        return this.exceptionCaught;
    }
}

