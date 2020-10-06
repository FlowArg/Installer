package net.minecraftforge.installer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.google.common.base.Predicates;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class SimpleInstaller
{
    public static void main(String[] args) throws IOException
    {
        setupLogger();
        if (System.getProperty("java.net.preferIPv4Stack") == null) //This is a dirty hack, but screw it, i'm hoping this as default will fix more things then it breaks.
        {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }
        System.out.println("java.net.preferIPv4Stack=" + System.getProperty("java.net.preferIPv4Stack"));

        OptionParser parser = new OptionParser();
        OptionSpec<File> clientOption = parser.accepts("installClient", "Install a client to the current directory").withOptionalArg().ofType(File.class).defaultsTo(new File("."));
        OptionSet optionSet = parser.parse(args);
        handleOptions(parser, optionSet, clientOption);        
    }

    private static void handleOptions(OptionParser parser, OptionSet optionSet, OptionSpec<File> clientOption) throws IOException
    {
        String path = VersionInfo.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (path.contains("!/"))
        {
            System.out.println("Due to java limitation, please do not run this jar in a folder ending with !");
            System.out.println(path);
            return;
        }

        if (optionSet.has(clientOption))
        {
        	VersionInfo.getVersionTarget();
            System.out.println("Installing client to current directory");
            if (!new ClientInstall().run(clientOption.value(optionSet), Predicates.alwaysFalse()))
            {
                System.err.println("There was an error during client installation");
                System.exit(1);
            }
            System.exit(0);
            
        }
        else
        {
            parser.printHelpOn(System.err);
        }
    }

    private static void setupLogger()
    {
        File f = new File(VersionInfo.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        File output;
        if (f.isFile()) output = new File(f.getName() + ".log");
        else            output = new File("installer.log");

        try
        {
            System.out.println("Setting up logger: " + output.getAbsolutePath());
            OutputStream fout = new BufferedOutputStream(new FileOutputStream(output));
            System.setOut(new PrintStream(new MultiOutputStream(System.out, fout), true));
            System.setErr(new PrintStream(new MultiOutputStream(System.err, fout), true));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private static class MultiOutputStream extends OutputStream
    {
        OutputStream[] outs;
        MultiOutputStream(OutputStream... outs)
        {
            this.outs = outs;
        }

        @Override
        public void write(int b) throws IOException
        {
            for (OutputStream out : outs)
                out.write(b);
        }

        @Override
        public void write(byte b[]) throws IOException
        {
            for (OutputStream out : outs)
                out.write(b);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException
        {
            for (OutputStream out : outs)
                out.write(b, off, len);
        }

        @Override
        public void flush() throws IOException
        {
            for (OutputStream out : outs)
                out.flush();
        }

        @Override
        public void close() throws IOException
        {
            for (OutputStream out : outs)
                out.close();
        }
    }

}
