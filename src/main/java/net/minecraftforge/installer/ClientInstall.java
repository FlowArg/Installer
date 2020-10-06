package net.minecraftforge.installer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import argo.format.PrettyJsonFormatter;
import argo.jdom.JsonNodeFactories;
import argo.jdom.JsonRootNode;
import argo.jdom.JsonStringNode;

public class ClientInstall implements ActionType
{
    private List<Artifact> grabbed;

    @Override
    public boolean run(File target, Predicate<String> optionals)
    {
        if (!target.exists())
        {
        	System.out.println("There is no minecraft installation at this location!");
            return false;
        }        
        File librariesDir = new File(target, "libraries");
        IMonitor monitor = DownloadUtils.buildMonitor();
        List<LibraryInfo> libraries = VersionInfo.getLibraries("clientreq", optionals);
        File versionJsonFile = new File(target, VersionInfo.getVersionTarget( )+ ".json");

        File targetLibraryFile = VersionInfo.getLibraryPath(librariesDir);
        grabbed = Lists.newArrayList();
        List<Artifact> bad = Lists.newArrayList();
        DownloadUtils.downloadInstalledLibraries(true, librariesDir, monitor, libraries, grabbed, bad);

        monitor.close();
        if (bad.size() > 0)
        {
            String list = Joiner.on("\n").join(bad);
            System.out.println("These libraries failed to download. Try again.\n"+list);
            return false;
        }

        if (!targetLibraryFile.getParentFile().mkdirs() && !targetLibraryFile.getParentFile().isDirectory())
        {
            if (!targetLibraryFile.getParentFile().delete())
            {
            	System.out.println("There was a problem with the launcher version data. You will need to clear "+targetLibraryFile.getAbsolutePath()+" manually");
                return false;
            }
            else
            {
                targetLibraryFile.getParentFile().mkdirs();
            }
        }

        String modListType = VersionInfo.getModListType();
        File modListFile = new File(target, "mods/mod_list.json");

        JsonRootNode versionJson = JsonNodeFactories.object(VersionInfo.getVersionInfo().getFields());

        if ("absolute".equals(modListType))
        {
            modListFile = new File(target, "mod_list.json");
            JsonStringNode node = (JsonStringNode)versionJson.getNode("minecraftArguments");
            try {
                Field value = JsonStringNode.class.getDeclaredField("value");
                value.setAccessible(true);
                String args = (String)value.get(node);
                value.set(node, args + " --modListFile \"absolute:"+modListFile.getAbsolutePath()+ "\"");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!"none".equals(modListType))
        {
            if (!OptionalLibrary.saveModListJson(librariesDir, modListFile, VersionInfo.getOptionals(), optionals))
            {
            	System.out.println("Failed to write mod_list.json, optional mods may not be loaded.");
            }
        }

        try
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Writer writer = new OutputStreamWriter(bos, Charsets.UTF_8);
            PrettyJsonFormatter.fieldOrderPreservingPrettyJsonFormatter().format(versionJson, writer);
            writer.close();

            byte[] output = bos.toByteArray();

            List<OptionalLibrary> lst = Lists.newArrayList();
            for (OptionalLibrary opt : VersionInfo.getOptionals())
            {
                if (optionals.apply(opt.getArtifact()) && opt.isInjected())
                    lst.add(opt);
            }

            if (lst.size() > 0)
            {
                BufferedReader reader = new BufferedReader(new StringReader(new String(output, Charsets.UTF_8)));
                bos = new ByteArrayOutputStream();
                PrintWriter printer = new PrintWriter(new OutputStreamWriter(bos, Charsets.UTF_8));
                String line = null;
                String prefix = null;
                boolean added = false;
                while ((line = reader.readLine()) != null)
                {
                    if (added)
                    {
                        printer.println(line);
                    }
                    else
                    {
                        if (line.contains("\"libraries\": ["))
                        {
                            prefix = line.substring(0, line.indexOf('"'));
                        }
                        else if (prefix != null && line.startsWith(prefix + "]"))
                        {
                            printer.println(prefix + "\t,");
                            for (int x = 0; x < lst.size(); x++)
                            {
                                OptionalLibrary opt = lst.get(x);
                                printer.println(prefix + "\t{");
                                printer.println(prefix + "\t\t\"name\": \"" + opt.getArtifact() + "\",");
                                printer.println(prefix + "\t\t\"url\": \"" + opt.getMaven() + "\"");
                                if (x < lst.size() - 1)
                                    printer.println(prefix + "\t},");
                                else
                                    printer.println(prefix + "\t}");
                            }
                            added = true;
                        }
                        printer.println(line);
                    }
                }

                printer.close();
                output = bos.toByteArray();
            }

            Files.write(output, versionJsonFile);
        }
        catch (Exception e)
        {
        	System.out.println("There was a problem writing the launcher version data,  is it write protected?");
            return false;
        }

        try
        {
            VersionInfo.extractFile(targetLibraryFile);
        }
        catch (IOException e)
        {
        	System.out.println("There was a problem writing the system library file");
            return false;
        }

        return true;
    }

    @Override
    public boolean isPathValid(File targetDir)
    {
        return targetDir.exists();
    }


    @Override
    public String getFileError(File targetDir)
    {
        if (targetDir.exists())
        {
            return "The directory is missing a launcher profile. Please run the minecraft launcher first";
        }
        else
        {
            return "There is no minecraft directory set up. Either choose an alternative, or run the minecraft launcher to create one";
        }
    }

    @Override
    public String getSuccessMessage()
    {
        if (grabbed.size() > 0)
        {
            return String.format("Successfully installed client profile %s for version %s into launcher and grabbed %d required libraries", VersionInfo.getProfileName(), VersionInfo.getVersion(), grabbed.size());
        }
        return String.format("Successfully installed client profile %s for version %s into launcher", VersionInfo.getProfileName(), VersionInfo.getVersion());
    }

    @Override
    public String getSponsorMessage()
    {
        return MirrorData.INSTANCE.hasMirrors() ? String.format("<html><a href=\'%s\'>Data kindly mirrored by %s</a></html>", MirrorData.INSTANCE.getSponsorURL(),MirrorData.INSTANCE.getSponsorName()) : null;
    }
}
