/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.jlan;

import android.content.Context;
import android.net.LinkAddress;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.Nullable;

import org.filesys.debug.DebugConfigSection;
import org.filesys.netbios.NetworkSettings;
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
import org.filesys.server.core.SharedDevice;
import org.filesys.server.filesys.DiskDeviceContext;
import org.filesys.server.filesys.DiskInterface;
import org.filesys.server.filesys.DiskSharedDevice;
import org.filesys.server.filesys.FilesystemsConfigSection;
import org.filesys.smb.server.SMBConfigSection;
import org.filesys.smb.server.SMBSrvSession;
import org.springframework.extensions.config.element.GenericConfigElement;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;

import de.buttercookie.simbadroid.util.FileUtils;
import de.buttercookie.simbadroid.util.SdCard;

public class JLANFileServerConfiguration extends ServerConfiguration {
    private static final int DefaultThreadPoolInit = 6;
    private static final int DefaultThreadPoolMax = 6;

    private static final int[] DefaultMemoryPoolBufSizes = {256, 4096, 16384, 66000};
    private static final int[] DefaultMemoryPoolInitAlloc = {20, 20, 5, 5};
    private static final int[] DefaultMemoryPoolMaxAlloc = {100, 50, 50, 50};

    public JLANFileServerConfiguration(Context context, String hostName)
            throws InvalidConfigurationException, DeviceContextException {
        super(hostName);

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
        final UserAccountList userAccounts = new UserAccountList();
        secConfig.setUserAccounts(userAccounts);

        // Shares
        FilesystemsConfigSection filesysConfig = new FilesystemsConfigSection(this);
        DiskInterface diskInterface = new SimbaDiskDriver();
        File sdCard = SdCard.findSdCardPath(context, null);
        if (sdCard != null) {
            addShare(diskInterface, this, filesysConfig, secConfig,
                    "External", sdCard.getAbsolutePath(),
                    FileUtils.getTrashcanPath(context, sdCard).getAbsolutePath(), true);
        }
        File internal = Environment.getExternalStorageDirectory();
        addShare(diskInterface, this, filesysConfig, secConfig,
                "Internal", internal.getAbsolutePath(),
                FileUtils.getTrashcanPath(context, internal).getAbsolutePath(), true);

        // SMB
        SMBConfigSection smbConfig = new SMBConfigSection(this);
        smbConfig.setServerName(hostName);
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // Core lib desugaring is missing some bits in the network code
            smbConfig.setDisableNIOCode(true);
            // Core lib desugaring doesn't handle HashMap's new KeySetView usage.
            smbConfig.setDisableHashedOpenFileMap(true);
        }
    }

    void setBindAddress(LinkAddress bindAddress) throws InvalidConfigurationException {
        final SMBConfigSection smbConfig =
                (SMBConfigSection) getConfigSection(SMBConfigSection.SectionName);

        smbConfig.setSMBBindAddress(bindAddress.getAddress());
        smbConfig.setNetBIOSBindAddress(bindAddress.getAddress());

        String broadcastAddress = getBroadcastAddress(bindAddress);
        smbConfig.setBroadcastMask(broadcastAddress);
        NetworkSettings.setBroadcastMask(broadcastAddress);
    }

    private static String getBroadcastAddress(LinkAddress address) {
        String broadcastAddress = null;
        if (address.getAddress() instanceof Inet4Address v4addr) {
            try {
                InterfaceAddress ifAddr = convertToInterfaceAddress(v4addr);
                broadcastAddress = ifAddr.getBroadcast().getHostAddress();
            } catch (SocketException ignored) {}
        } else if (address.getAddress() instanceof Inet6Address) {
            broadcastAddress = "ff02::1";
        }
        return broadcastAddress;
    }

    private static InterfaceAddress convertToInterfaceAddress(InetAddress address) throws SocketException {
        List<InterfaceAddress> interfaceAddresses =
                NetworkInterface.getByInetAddress(address).getInterfaceAddresses();
        return interfaceAddresses.stream().reduce((addr1, addr2) ->
                addr2.getAddress().equals(address) ? addr2 : addr1).get();
    }

    private static void addShare(DiskInterface diskInterface,
                                 ServerConfiguration serverConfig,
                                 FilesystemsConfigSection filesysConfig,
                                 SecurityConfigSection secConfig,
                                 String shareName, String sharePath, @Nullable String trashcanPath,
                                 boolean caseInsensitive)
            throws DeviceContextException {
        final GenericConfigElement driverConfig = new GenericConfigElement("driver");
        final GenericConfigElement localPathConfig = new GenericConfigElement("LocalPath");
        localPathConfig.setValue(sharePath);
        driverConfig.addChild(localPathConfig);
        if (trashcanPath != null) {
            final GenericConfigElement trashcanPathConfig =
                    new GenericConfigElement("TrashcanPath");
            trashcanPathConfig.setValue(trashcanPath);
            driverConfig.addChild(trashcanPathConfig);
        }
        if (caseInsensitive) {
            driverConfig.addChild(new GenericConfigElement("DiskIsCaseInsensitive"));
        }
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

    void removeTrashcanFolders() {
        final FilesystemsConfigSection filesysConfig =
                (FilesystemsConfigSection) getConfigSection(FilesystemsConfigSection.SectionName);

        Enumeration<SharedDevice> shares = filesysConfig.getShares().enumerateShares();
        while (shares.hasMoreElements()) {
            SharedDevice share = shares.nextElement();
            if (share.getContext() instanceof SimbaDiskDeviceContext diskContext) {
                diskContext.removeTrashcanFolderIfEmpty();
            }
        }
    }
}
