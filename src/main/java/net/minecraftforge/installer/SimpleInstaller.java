/*
 * Installer
 * Copyright (c) 2016-2018.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.minecraftforge.installer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.swing.UIManager;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraftforge.installer.actions.ClientInstall;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.Util;

public class SimpleInstaller
{
    public static boolean headless = false;

    public static void main(String[] args) throws IOException
    {
        ProgressCallback monitor;
        try
        {
            monitor = ProgressCallback.withOutputs(System.out, getLog());
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            monitor = ProgressCallback.withOutputs(System.out);
        }
        hookStdOut(monitor);

        if (System.getProperty("java.net.preferIPv4Stack") == null) //This is a dirty hack, but screw it, i'm hoping this as default will fix more things then it breaks.
        {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }
        String vendor = System.getProperty("java.vendor", "missing vendor");
        String javaVersion = System.getProperty("java.version", "missing java version");
        String jvmVersion = System.getProperty("java.vm.version", "missing jvm version");
        monitor.message(String.format("JVM info: %s - %s - %s", vendor, javaVersion, jvmVersion));
        monitor.message("java.net.preferIPv4Stack=" + System.getProperty("java.net.preferIPv4Stack"));

        String path = SimpleInstaller.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (path.contains("!/"))
        {
            monitor.stage("Due to java limitation, please do not run this jar in a folder ending with !");
            monitor.message(path);
            return;
        }

        OptionParser parser = new OptionParser();
        OptionSpec<File> clientInstallOption = parser.accepts("installClient", "Install a client to the specified directory").withOptionalArg().ofType(File.class).defaultsTo(new File("."));
        OptionSpec<Void> helpOption = parser.acceptsAll(Arrays.asList("h", "help"),"Help with this installer");
        OptionSpec<Void> noguiOption = parser.accepts("nogui");
        OptionSet optionSet = parser.parse(args);

        if (optionSet.has(helpOption)) {
            parser.printHelpOn(System.out);
            return;
        }
        final boolean bool = optionSet.has(clientInstallOption);
        launchGui(monitor, bool, bool ? clientInstallOption.value(optionSet) : null, optionSet.has(noguiOption));
    }

    private static void launchGui(ProgressCallback monitor, boolean installClient, File  dirToInstall, boolean noGui)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e)
        {
        }

        Install profile = Util.loadInstallProfile();
        InstallerPanel panel = new InstallerPanel(dirToInstall, profile);
        panel.run(monitor, installClient ? new ClientInstall(profile, monitor) : null, noGui);
    }

    private static OutputStream getLog() throws FileNotFoundException
    {
        File f = new File(SimpleInstaller.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        File output;
        if (f.isFile()) output = new File(f.getName() + ".log");
        else            output = new File("installer.log");

        return new BufferedOutputStream(new FileOutputStream(output));
    }

    static void hookStdOut(ProgressCallback monitor)
    {
        final Pattern endingWhitespace = Pattern.compile("\\r?\\n$");
        final OutputStream monitorStream = new OutputStream() {

            @Override
            public void write(byte[] buf, int off, int len)
            {
                byte[] toWrite = new byte[len];
                System.arraycopy(buf, off, toWrite, 0, len);
                write(toWrite);
            }

            @Override
            public void write(byte[] b)
            {
                String toWrite = new String(b);
                toWrite = endingWhitespace.matcher(toWrite).replaceAll("");
                if (!toWrite.isEmpty()) {
                    monitor.message(toWrite);
                }
            }

            @Override
            public void write(int b)
            {
                write(new byte[] { (byte) b });
            }
        };

        System.setOut(new PrintStream(monitorStream));
        System.setErr(new PrintStream(monitorStream));
    }
}
