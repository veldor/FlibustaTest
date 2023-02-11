package net.veldor.tor_client.model.control

import android.content.Context
import android.util.Log
import net.veldor.tor_client.model.listeners.BootstrapLoadProgressListener
import net.veldor.tor_client.model.tor_utils.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Volatile
private var controlPort = 0

class AndroidOnionProxyManager(val context: Context) {

    var bootstrapLoadProgressListener: BootstrapLoadProgressListener? = null
    private var isInterrupted = false
    private var launchInProgress = false
    val lastLog: String?
    get() {return controlConnection?.lastLog}
    val lastBootstrapLog: String?
    get() {return controlConnection?.lastBootstrapLog}

    @Volatile
    private var controlSocket: Socket? = null

    // If controlConnection is not null then this means that a connection exists and the Tor OP will die when
    // the connection fails.
    @Volatile
    private var controlConnection: TorControlConnection? = null

    /**
     * Determines if the boot strap process has completed.
     * @return True if complete
     */
    @get:Synchronized
    val isBootstrapped: Boolean
        get() {
            if (controlConnection == null) {
                return false
            }
            var phase: String? = null
            try {
                phase = controlConnection!!.getInfo("status/bootstrap-phase")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.d("surprise", "AndroidOnionProxyManager: 24 Control connection is not responding properly to getInfo ${e.message}")
            }
            return if (phase != null && phase.contains("PROGRESS=100")) {
                Log.d("surprise", "AndroidOnionProxyManager: 26 Tor has already bootstrapped\"")
                true
            } else false
        }

    @get:Synchronized
    val isNetworkEnabled: Boolean
        get() {
            if (controlConnection == null) {
                throw RuntimeException("Tor is not running!")
            }
            val disableNetworkSettingValues = controlConnection!!.getConf("DisableNetwork")
            var result = false
            // It's theoretically possible for us to get multiple values back, if even one is false then we will
            // assume all are false
            for (configEntry in disableNetworkSettingValues) {
                result = if (configEntry.value == "1") {
                    return false
                } else {
                    true
                }
            }
            return result
        }

    fun startWithRepeat(secondsBeforeTimeOut: Int, numberOfRetries: Int): Boolean {
        isInterrupted = false
        launchInProgress = true
        require(!(secondsBeforeTimeOut <= 0 || numberOfRetries < 0)) { "secondsBeforeTimeOut >= 0 & numberOfRetries > 0" }
        return try {
            for (retryCount in 0 until numberOfRetries) {
                if (!installAndStartTorOp()) {
                    return false
                }
                Log.d("surprise", "AndroidOnionProxyManager: 76 tor files installed")
                enableNetwork(true)

                // We will check every second to see if boot strapping has finally finished
                for (secondsWaited in 0 until secondsBeforeTimeOut) {
                    bootstrapLoadProgressListener?.tick(secondsBeforeTimeOut, secondsWaited, lastBootstrapLog)
                    if(isInterrupted){
                        Log.d("surprise", "AndroidOnionProxyManager: 91 launch interrupted")
                        isInterrupted = false
                        stop()
                        androidOnionProxyContext.deleteAllFilesButHiddenServices()
                        return false
                    }
                    if (!isBootstrapped) {
                        Thread.sleep(1000, 0)
                    } else {
                        return true
                    }
                }
                // Bootstrapping isn't over so we need to restart and try again
                stop()
                // Experimentally we have found that if a Tor OP has run before and thus has cached descriptors
                // and that when we try to start it again it won't start then deleting the cached data can fix this.
                // But, if there is cached data and things do work then the Tor OP will start faster than it would
                // if we delete everything.
                // So our compromise is that we try to start the Tor OP 'as is' on the first round and after that
                // we delete all the files.
                androidOnionProxyContext.deleteAllFilesButHiddenServices()
            }
            false
        } finally {
            // Make sure we return the Tor OP in some kind of consistent state, even if it's 'off'.
            if (!isRunning) {
                stop()
            }
            launchInProgress = false
        }
    }

    private fun installAndStartTorOp(): Boolean {
        // The Tor OP will die if it looses the connection to its socket so if there is no controlSocket defined
        // then Tor is dead. This assumes, of course, that takeOwnership works and we can't end up with Zombies.
        if (controlConnection != null) {
            LOG.info("Tor is already running")
            Log.d("surprise", "AndroidOnionProxyManager: 114 Tor is already running")
            return true
        }
        androidOnionProxyContext.clearInstallFiles()



        Log.d("surprise", "AndroidOnionProxyManager: 119 files installed")

        val cookieFile = androidOnionProxyContext.cookieFile
        if (
            cookieFile.parentFile != null &&
            !cookieFile.parentFile!!.exists() &&
            !cookieFile.parentFile!!.mkdirs()
        ) {
            throw RuntimeException("Could not create cookieFile parent directory")
        }

        Log.d("surprise", "AndroidOnionProxyManager: 130 cookie file ready")

        // The original code from Briar watches individual files, not a directory and Android's file observer
        // won't work on files that don't exist. Rather than take 5 seconds to rewrite Briar's code I instead
        // just make sure the file exists
        if (!cookieFile.exists() && !cookieFile.createNewFile()) {
            throw RuntimeException("Could not create cookieFile")
        }
        // Watch for the auth cookie file being created/updated
        val cookieObserver = androidOnionProxyContext.generateWriteObserver(cookieFile)
        // Start a new Tor process
        val torPath = androidOnionProxyContext.torExecutableFile.absolutePath
        val configPath = androidOnionProxyContext.torConfigFile.absolutePath
        val pid = androidOnionProxyContext.processId
        val cmd = arrayOf(torPath, "-f", configPath, OWNER, pid)
        val processBuilder = ProcessBuilder(*cmd)
        androidOnionProxyContext.setEnvironmentArgsAndWorkingDirectoryForStart(processBuilder)
        var torProcess: Process? = null
        return try {
//            torProcess = Runtime.getRuntime().exec(cmd, env, workingDirectory);
            torProcess = processBuilder.start()
            val controlPortCountDownLatch = CountDownLatch(1)
            eatStream(torProcess.inputStream, false, controlPortCountDownLatch)
            eatStream(torProcess.errorStream, true, null)

            // On platforms other than Windows we run as a daemon and so we need to wait for the process to detach
            // or exit. In the case of Windows the equivalent is running as a service and unfortunately that requires
            // managing the service, such as turning it off or uninstalling it when it's time to move on. Any number
            // of errors can prevent us from doing the cleanup and so we would leave the process running around. Rather
            // than do that on Windows we just let the process run on the exec and hence don't look for an exit code.
            // This does create a condition where the process has exited due to a problem but we should hopefully
            // detect that when we try to use the control connection.
            if (OsData.osType != OsData.OsType.WINDOWS) {
                val exit = torProcess.waitFor()
                torProcess = null
                if (exit != 0) {
                    //LOG.warn("Tor exited with value " + exit);
                    return false
                }
            }
            Log.d("surprise", "AndroidOnionProxyManager: 170 tor starter process done job")
            // Wait for the auth cookie file to be created/updated
            if (!cookieObserver.poll(COOKIE_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)) {
                //LOG.warn("Auth cookie not created");
                Log.d("surprise", "AndroidOnionProxyManager: 174 Auth cookie not created")
                FileUtilities.listFilesToLog(androidOnionProxyContext.workingDirectory)
                return false
            }
            Log.d("surprise", "AndroidOnionProxyManager: 178 auth cookie created")
            // Now we should be able to connect to the new process
            controlPortCountDownLatch.await()
            Log.d("surprise", "AndroidOnionProxyManager: 181 creating control port connection")
            controlSocket = Socket("127.0.0.1", controlPort)
            Log.d("surprise", "AndroidOnionProxyManager: 182 control socket opened")
            // Open a control connection and authenticate using the cookie file
            val controlConnection = TorControlConnection(controlSocket)
            Log.d("surprise", "AndroidOnionProxyManager: 201 control connection created")
            val cookie = FileUtilities.read(cookieFile)
            Log.d("surprise", "AndroidOnionProxyManager: 203 $cookie")
            controlConnection.authenticate(cookie)
            Log.d("surprise", "AndroidOnionProxyManager: 203 control connection authenticated")
            // Tell Tor to exit when the control connection is closed
            controlConnection.takeOwnership()
            controlConnection.resetConf(listOf(OWNER))
            // Register to receive events from the Tor process
            controlConnection.setEventHandler(OnionProxyManagerEventHandler())
            controlConnection.setEvents(listOf(*EVENTS))

            // We only set the class property once the connection is in a known good state
            this.controlConnection = controlConnection
            Log.d("surprise", "AndroidOnionProxyManager: 195 control connection created")
            true
        } catch (e: SecurityException) {
            //LOG.warn(e.toString(), e);
            false
        } catch (e: InterruptedException) {
            //LOG.warn("Interrupted while starting Tor", e);
            Thread.currentThread().interrupt()
            false
        }
        catch (t: Throwable){
            t.printStackTrace()
            Log.d("surprise", "AndroidOnionProxyManager: 208 have exception")
            false
        }
        finally {
            if (controlConnection == null && torProcess != null) {
                // It's possible that something 'bad' could happen after we executed exec but before we takeOwnership()
                // in which case the Tor OP will hang out as a zombie until this process is killed. This is problematic
                // when we want to do things like
                torProcess.destroy()
            }
        }
    }


    /**
     * Kills the Tor OP Process. Once you have called this method nothing is going to work until you either call
     * startWithRepeat or installAndStartTorOp
     * @throws IOException - File errors
     */
    @Synchronized
    @Throws(IOException::class)
     fun stop() {
        try {
            if (controlConnection == null) {
                return
            }
            controlConnection!!.setConf("DisableNetwork", "1")
            controlConnection!!.shutdownTor("TERM")
        } finally {
            if (controlSocket != null) {
                controlSocket!!.close()
            }
            controlConnection = null
            controlSocket = null
        }
    }


    private fun eatStream(
        inputStream: InputStream,
        stdError: Boolean,
        countDownLatch: CountDownLatch?
    ) {
        object : Thread() {
            override fun run() {
                val scanner = Scanner(inputStream)
                try {
                    while (scanner.hasNextLine()) {
                        if (stdError) {
                            //LOG.error(scanner.nextLine());
                            Log.d(
                                "surprise",
                                "AndroidOnionProxyManager: 251 have error ${scanner.nextLine()}"
                            )
                        } else {
                            val nextLine = scanner.nextLine()
                            // We need to find the line where it tells us what the control port is.
                            // The line that will appear in stdio with the control port looks like:
                            // Control listener listening on port 39717.
                            if (nextLine.contains("Opening Control listener ")) {
                                // For the record, I hate regex so I'm doing this manually
                                controlPort =
                                    nextLine.substring(
                                        nextLine.lastIndexOf(":") + 1
                                    ).toInt()
                                Log.d(
                                    "surprise",
                                    "AndroidOnionProxyManager: 267 control port is $controlPort"
                                )
                                countDownLatch!!.countDown()
                            }
                            //LOG.info(nextLine);
                        }
                    }
                } finally {
                    try {
                        inputStream.close()
                    } catch (e: IOException) {
                        //LOG.error("", e);
                        e.printStackTrace()
                        Log.d("surprise", "AndroidOnionProxyManager: 265 Couldn't close input stream in eatStream ${e.message}")
                    }
                }
            }
        }.start()
    }

    /**
     * Tells the Tor OP if it should accept network connections
     * @param enable If true then the Tor OP will accept SOCKS connections, otherwise not.
     * @throws IOException - IO exceptions
     */
    @Synchronized
    @Throws(IOException::class)
    fun enableNetwork(enable: Boolean) {
        if (controlConnection == null) {
            throw RuntimeException("Tor is not running!")
        }
        //LOG.info("Enabling network: " + enable);
        controlConnection!!.setConf("DisableNetwork", if (enable) "0" else "1")
    }

    @get:Synchronized
    val isLaunchInProgress: Boolean
    get() {return launchInProgress}

    @get:Synchronized
    val isRunning: Boolean
        get() = isBootstrapped && isNetworkEnabled

    private val androidOnionProxyContext: AndroidOnionProxyContext = AndroidOnionProxyContext(context)

    companion object{
        private val EVENTS = arrayOf(
            "CIRC", "ORCONN", "NOTICE", "WARN", "ERR"
        )
        const val OWNER = "__OwningControllerProcess"
        private const val COOKIE_TIMEOUT = 3 * 1000 // Milliseconds
        private val LOG: Logger = LoggerFactory.getLogger(AndroidOnionProxyManager::class.java)
    }

    fun interrupt(){
        isInterrupted = true
    }
}