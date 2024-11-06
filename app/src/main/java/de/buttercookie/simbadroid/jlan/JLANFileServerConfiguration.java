/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.jlan;

import android.content.Context;
import android.os.Environment;

import org.filesys.debug.DebugConfigSection;
import org.filesys.server.SrvSession;
import org.filesys.server.auth.ClientInfo;
import org.filesys.server.auth.ISMBAuthenticator;
import org.filesys.server.auth.LocalAuthenticator;
import org.filesys.server.auth.SMBAuthenticator;
import org.filesys.server.auth.UserAccountList;
import org.filesys.server.auth.acl.DefaultAccessControlManager;
import org.filesys.server.config.CoreServerConfigSection;
import org.filesys.server.config.GlobalConfigSection;
import org.filesys.server.config.InvalidConfigurationException;
import org.filesys.server.config.SecurityConfigSection;
import org.filesys.server.config.ServerConfiguration;
import org.filesys.server.core.DeviceContextException;
import org.filesys.server.filesys.DiskDeviceContext;
import org.filesys.server.filesys.DiskInterface;
import org.filesys.server.filesys.DiskSharedDevice;
import org.filesys.server.filesys.FilesystemsConfigSection;
import org.filesys.smb.server.SMBConfigSection;
import org.filesys.smb.server.SMBSrvSession;
import org.springframework.extensions.config.element.GenericConfigElement;

import java.util.EnumSet;

import de.buttercookie.simbadroid.util.FileUtils;

public class JLANFileServerConfiguration extends ServerConfiguration {
    private static final String HOSTNAME = "JLANHOST";

    private static final int DefaultThreadPoolInit = 6;
    private static final int DefaultThreadPoolMax = 6;

    private static final int[] DefaultMemoryPoolBufSizes = {256, 4096, 16384, 66000};
    private static final int[] DefaultMemoryPoolInitAlloc = {20, 20, 5, 5};
    private static final int[] DefaultMemoryPoolMaxAlloc = {100, 50, 50, 50};

    public JLANFileServerConfiguration(Context context)
            throws InvalidConfigurationException, DeviceContextException {
        super(HOSTNAME);
        setServerName(HOSTNAME);

        // Debug
        DebugConfigSection debugConfig = new DebugConfigSection(this);
        final GenericConfigElement debugConfigElement = new GenericConfigElement("output");
        final GenericConfigElement logLevelConfigElement = new GenericConfigElement("logLevel");
        logLevelConfigElement.setValue("Debug");
        debugConfig.setDebug("org.filesys.debug.ConsoleDebug", debugConfigElement);

        // Core
        CoreServerConfigSection coreConfig = new CoreServerConfigSection(this);
        coreConfig.setMemoryPool(DefaultMemoryPoolBufSizes, DefaultMemoryPoolInitAlloc,
                DefaultMemoryPoolMaxAlloc);
        coreConfig.setThreadPool(DefaultThreadPoolInit, DefaultThreadPoolMax);
        coreConfig.getThreadPool().setDebug(false);

        // Global
        GlobalConfigSection globalConfig = new GlobalConfigSection(this);

        // Security
        SecurityConfigSection secConfig = new SecurityConfigSection(this);
        DefaultAccessControlManager accessControlManager = new DefaultAccessControlManager();
        accessControlManager.setDebug(false);
        accessControlManager.initialize(this, new GenericConfigElement("aclManager"));
        secConfig.setAccessControlManager(accessControlManager);
        secConfig.setJCEProvider("org.bouncycastle.jce.provider.BouncyCastleProvider");
        final UserAccountList userAccounts = new UserAccountList();
        secConfig.setUserAccounts(userAccounts);

        // Shares
        FilesystemsConfigSection filesysConfig = new FilesystemsConfigSection(this);
        DiskInterface diskInterface = new DiskDriver();
        addShare(diskInterface, this, filesysConfig, secConfig,
                "External", FileUtils.getStoragePath(context));
        addShare(diskInterface, this, filesysConfig, secConfig,
                "Internal", Environment.getExternalStorageDirectory().toString());

        // SMB
        SMBConfigSection smbConfig = new SMBConfigSection(this);
        smbConfig.setServerName(HOSTNAME);
        smbConfig.setDomainName("WORKGROUP");
        smbConfig.setHostAnnounceInterval(5);
        smbConfig.setHostAnnouncer(true);
        smbConfig.setNameServerPort(1137);
        smbConfig.setDatagramPort(1138);
        smbConfig.setSessionPort(1139);
        smbConfig.setTcpipSMB(true);
        smbConfig.setTcpipSMBPort(4450);
        final SMBAuthenticator authenticator = new LocalAuthenticator() {
            @Override
            public AuthStatus authenticateUser(ClientInfo client, SrvSession sess,
                                               PasswordAlgorithm alg) {
                return AuthStatus.AUTHENTICATED;
            }
        };
        authenticator.setDebug(false);
        authenticator.setAllowGuest(true);
        authenticator.setAccessMode(ISMBAuthenticator.AuthMode.USER);
        final GenericConfigElement authenticatorConfigElement =
                new GenericConfigElement("authenticator");
        authenticator.initialize(this, authenticatorConfigElement);
        smbConfig.setAuthenticator(authenticator);
        smbConfig.setNetBIOSDebug(false);
        smbConfig.setHostAnnounceDebug(false);
        smbConfig.setSessionDebugFlags(EnumSet.noneOf(SMBSrvSession.Dbg.class));
    }

    private static void addShare(DiskInterface diskInterface,
                                 ServerConfiguration serverConfig,
                                 FilesystemsConfigSection filesysConfig,
                                 SecurityConfigSection secConfig,
                                 String shareName, String sharePath)
            throws DeviceContextException {
        final GenericConfigElement driverConfig = new GenericConfigElement("driver");
        final GenericConfigElement localPathConfig = new GenericConfigElement("LocalPath");
        localPathConfig.setValue(sharePath);
        driverConfig.addChild(localPathConfig);
        DiskDeviceContext diskDeviceContext =
                (DiskDeviceContext) diskInterface.createContext(shareName, driverConfig);
        diskDeviceContext.setShareName(shareName);
        diskDeviceContext.setConfigurationParameters(driverConfig);
        diskDeviceContext.enableChangeHandler(false);
        DiskSharedDevice diskDev = new DiskSharedDevice(shareName, diskInterface, diskDeviceContext);
        diskDev.setConfiguration(serverConfig);
        diskDev.setAccessControlList(secConfig.getGlobalAccessControls());
        diskDeviceContext.startFilesystem(diskDev);
        filesysConfig.addShare(diskDev);
    }

}
