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

import java.awt.Frame;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.swing.JOptionPane;

import net.minecraftforge.installer.actions.Action;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.OptionalLibrary;

public class InstallerPanel{
    private File targetDir;
    private List<OptionalListEntry> optionals = new ArrayList<>();
    private Install profile;

    public InstallerPanel(File targetDir, Install profile)
    {
        this.profile = profile;
        this.targetDir = targetDir;

        if (this.profile.getSpec() != 0) {
            JOptionPane.showMessageDialog(null, "Invalid launcher profile spec: " + profile.getSpec() + " Only 0 is supported, Whoever package this installer messed up.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public void run(ProgressCallback monitor, Action actionToAct, boolean noGui)
    {
    	Frame emptyFrame = null;
    	if(!noGui)
    	{
    		emptyFrame = new Frame("Mod system installer");
            emptyFrame.setLocationRelativeTo(null);
            emptyFrame.setUndecorated(true);
            emptyFrame.setVisible(true);
    	}

    	ProgressFrame prog = null;
    	if(!noGui)
    	{
            prog = new ProgressFrame(monitor, "Installing " + profile.getVersion(), Thread.currentThread()::interrupt);
            SimpleInstaller.hookStdOut(prog);
    	}
    	else monitor.start("Installing " + profile.getVersion());

        final Predicate<String> optPred = input -> {
            Optional<OptionalListEntry> ent = this.optionals.stream().filter(e -> e.lib.getArtifact().equals(input)).findFirst();
            return !ent.isPresent() || ent.get().isEnabled();
        };
        
        Action action = actionToAct;
        try {
        	if(prog != null)
        	{
                prog.setVisible(true);
                prog.toFront();
        	}
            if (action.run(targetDir, optPred)) {
            	if(prog != null)
            	{
                    prog.start("Finished!");
                    prog.progress(1);
            	}
            	else monitor.start("Finished!");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
        	if(prog != null)
        		prog.dispose();
            SimpleInstaller.hookStdOut(monitor);
        }
        
        if(emptyFrame != null)
        	emptyFrame.dispose();        
    }

    private static class OptionalListEntry
    {
        OptionalLibrary lib;
        private boolean enabled = false;

        public boolean isEnabled(){ return this.enabled; }
    }
}
