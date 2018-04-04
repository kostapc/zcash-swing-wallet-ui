/************************************************************************************************
 *  _________          _     ____          _           __        __    _ _      _   _   _ ___ 
 * |__  / ___|__ _ ___| |__ / ___|_      _(_)_ __   __ \ \      / /_ _| | | ___| |_| | | |_ _|
 *   / / |   / _` / __| '_ \\___ \ \ /\ / / | '_ \ / _` \ \ /\ / / _` | | |/ _ \ __| | | || | 
 *  / /| |__| (_| \__ \ | | |___) \ V  V /| | | | | (_| |\ V  V / (_| | | |  __/ |_| |_| || | 
 * /____\____\__,_|___/_| |_|____/ \_/\_/ |_|_| |_|\__, | \_/\_/ \__,_|_|_|\___|\__|\___/|___|
 *                                                 |___/                                      
 *                                       
 * Copyright (c) 2016 Ivan Vaklinov <ivan@vaklinov.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 **********************************************************************************/
package com.vaklinov.zcashui;

import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;


/**
 * Provides miscellaneous operations for the wallet file.
 * 
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class WalletOperations
{	
	private ZCashUI parent;
	private JTabbedPane tabs;
	private DashboardPanel dashboard;
	private SendCashPanel  sendCash;
	private AddressesPanel addresses;
	
	private ZCashInstallationObserver installationObserver;
	private ZCashClientCaller         clientCaller;
	private StatusUpdateErrorReporter errorReporter;

    private ResourceBundleUTF8 rb = ResourceBundleUTF8.getResourceBundle();

    
	public WalletOperations(ZCashUI parent,
			                JTabbedPane tabs,
			                DashboardPanel dashboard,
			                AddressesPanel addresses,
			                SendCashPanel  sendCash,
			                
			                ZCashInstallationObserver installationObserver, 
			                ZCashClientCaller clientCaller,
			                StatusUpdateErrorReporter errorReporter) 
        throws IOException, InterruptedException, WalletCallException 
	{
		this.parent    = parent;
		this.tabs      = tabs;
		this.dashboard = dashboard;
		this.addresses = addresses;
		this.sendCash  = sendCash;
		
		this.installationObserver = installationObserver;
		this.clientCaller = clientCaller;
		this.errorReporter = errorReporter;
	}

	
	public void encryptWallet()
	{
		try
		{			
			if (this.clientCaller.isWalletEncrypted())
			{
		        JOptionPane.showMessageDialog(
		            this.parent,
		            rb.S("The wallet.dat file being used is already encrypted. ") +
		            rb.S("This \noperation may be performed only on a wallet that ") + 
		            rb.S("is not\nyet encrypted!"),
		            rb.S("Wallet is already encrypted..."),
		            JOptionPane.ERROR_MESSAGE);
		        return;
			}
			
			PasswordEncryptionDialog pd = new PasswordEncryptionDialog(this.parent);
			pd.setVisible(true);
			
			if (!pd.isOKPressed())
			{
				return;
			}
			
			Cursor oldCursor = this.parent.getCursor();
			try
			{
				
				this.parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				this.dashboard.stopThreadsAndTimers();
				this.sendCash.stopThreadsAndTimers();
				
				this.clientCaller.encryptWallet(pd.getPassword());
				
				this.parent.setCursor(oldCursor);
			} catch (WalletCallException wce)
			{
				this.parent.setCursor(oldCursor);
				Log.error("Unexpected error: ", wce);
				
				JOptionPane.showMessageDialog(
					this.parent, 
					rb.S("An unexpected error occurred while encrypting the wallet!\n") +
					rb.S("It is recommended to stop and restart both kotod and the GUI wallet! \n") +
					"\n" + wce.getMessage().replace(",", ",\n"),
					rb.S("Error in encrypting wallet..."), JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			JOptionPane.showMessageDialog(
				this.parent, 
				rb.S("The wallet has been encrypted sucessfully and kotod has stopped.\n") +
				rb.S("The GUI wallet will be stopped as well. Please restart both. In\n") +
				rb.S("addtion the internal wallet keypool has been flushed. You need\n") +
				rb.S("to make a new backup...") +
				"\n",
				rb.S("Wallet is now encrypted..."), JOptionPane.INFORMATION_MESSAGE);
			
			this.parent.exitProgram();
			
		} catch (Exception e)
		{
			this.errorReporter.reportError(e, false);
		}
	}
	
	
	public void backupWallet()
	{
		try
		{
			this.issueBackupDirectoryWarning();
			
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle(rb.S("Backup wallet to file..."));
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setCurrentDirectory(OSUtil.getUserHomeDirectory());
			 
			int result = fileChooser.showSaveDialog(this.parent);
			 
			if (result != JFileChooser.APPROVE_OPTION) 
			{
			    return;
			}
			
			File f = fileChooser.getSelectedFile();
			
			Cursor oldCursor = this.parent.getCursor();
			String path = null;
			try
			{
				this.parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							
				path = this.clientCaller.backupWallet(f.getName());
				
				this.parent.setCursor(oldCursor);
			} catch (WalletCallException wce)
			{
				this.parent.setCursor(oldCursor);
				Log.error("Unexpected error: ", wce);
				
				JOptionPane.showMessageDialog(
					this.parent, 
					rb.S("An unexpected error occurred while backing up the wallet!") +
					"\n" + wce.getMessage().replace(",", ",\n"),
					rb.S("Error in backing up wallet..."), JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			JOptionPane.showMessageDialog(
				this.parent, 
				rb.S("The wallet has been backed up successfully to file: ") + f.getName() + "\n" +
				rb.S("in the backup directory provided to kotod (-exportdir=<dir>).\nFull path is: ") + 
				path,
				rb.S("Wallet is backed up..."), JOptionPane.INFORMATION_MESSAGE);
			
		} catch (Exception e)
		{
			this.errorReporter.reportError(e, false);
		}
	}
	
	
	public void exportWalletPrivateKeys()
	{
		// TODO: Will need corrections once encryption is reenabled!!!
		
		try
		{
			this.issueBackupDirectoryWarning();
			
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle(rb.S("Export wallet private keys to file..."));
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setCurrentDirectory(OSUtil.getUserHomeDirectory());
			 
			int result = fileChooser.showSaveDialog(this.parent);
			 
			if (result != JFileChooser.APPROVE_OPTION) 
			{
			    return;
			}
			
			File f = fileChooser.getSelectedFile();
			
			Cursor oldCursor = this.parent.getCursor();
			String path = null;
			try
			{
				this.parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							
				path = this.clientCaller.exportWallet(f.getName());
				
				this.parent.setCursor(oldCursor);
			} catch (WalletCallException wce)
			{
				this.parent.setCursor(oldCursor);
				Log.error("Unexpected error: ", wce);
				
				JOptionPane.showMessageDialog(
					this.parent, 
					rb.S("An unexpected error occurred while exporting wallet private keys!") +
					"\n" + wce.getMessage().replace(",", ",\n"),
					rb.S("Error in exporting wallet private keys..."), JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			JOptionPane.showMessageDialog(
				this.parent, 
				rb.S("The wallet private keys have been exported successfully to file:\n") + 
				f.getName() + "\n" +
				rb.S("in the backup directory provided to kotod (-exportdir=<dir>).\nFull path is: ") + 
				path + "\n" +
				rb.S("You need to protect this file from unauthorized access. Anyone who\n") +
				rb.S("has access to the private keys can spend the Koto balance!"),
				rb.S("Wallet private key export..."), JOptionPane.INFORMATION_MESSAGE);
			
		} catch (Exception e)
		{
			this.errorReporter.reportError(e, false);
		}
	}

	
	public void importWalletPrivateKeys()
	{
		// TODO: Will need corrections once encryption is re-enabled!!!
		
	    int option = JOptionPane.showConfirmDialog(  
		    this.parent,
		    rb.S("Private key import is a potentially slow operation. It may take\n") +
		    rb.S("several minutes during which the GUI will be non-responsive.\n") +
		    rb.S("The data to import must be in the format used by the option:\n") +
		    rb.S("\"Export private keys...\"\n\n") +
		    rb.S("Are you sure you wish to import private keys?"),
		    rb.S("Private key import notice..."),
		    JOptionPane.YES_NO_OPTION);
		if (option == JOptionPane.NO_OPTION)
		{
		  	return;
		}
		
		try
		{
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle(rb.S("Import wallet private keys from file..."));
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			 
			int result = fileChooser.showOpenDialog(this.parent);
			 
			if (result != JFileChooser.APPROVE_OPTION) 
			{
			    return;
			}
			
			File f = fileChooser.getSelectedFile();
			
			Cursor oldCursor = this.parent.getCursor();
			try
			{
				this.parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							
				this.clientCaller.importWallet(f.getCanonicalPath());
				
				this.parent.setCursor(oldCursor);
			} catch (WalletCallException wce)
			{
				this.parent.setCursor(oldCursor);
				Log.error("Unexpected error: ", wce);
				
				JOptionPane.showMessageDialog(
					this.parent, 
					rb.S("An unexpected error occurred while importing wallet private keys!") +
					"\n" + wce.getMessage().replace(",", ",\n"),
					rb.S("Error in importing wallet private keys..."), JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			JOptionPane.showMessageDialog(
				this.parent, 
				rb.S("Wallet private keys have been imported successfully from location:\n") +
				f.getCanonicalPath() + "\n\n",
				rb.S("Wallet private key import..."), JOptionPane.INFORMATION_MESSAGE);
			
		} catch (Exception e)
		{
			this.errorReporter.reportError(e, false);
		}
	}
	
	
	public void showPrivateKey()
	{
		if (this.tabs.getSelectedIndex() != 1)
		{
			JOptionPane.showMessageDialog(
				this.parent, 
				rb.S("Please select an address in the \"Own addresses\" tab ") +
				rb.S("to view its private key"),
				rb.S("Please select an address..."), JOptionPane.INFORMATION_MESSAGE);
			this.tabs.setSelectedIndex(1);
			return;
		}
		
		String address = this.addresses.getSelectedAddress();
		
		if (address == null)
		{
			JOptionPane.showMessageDialog(
				this.parent, 
				rb.S("Please select an address in the table of addresses ") +
				rb.S("to view its private key"),
				rb.S("Please select an address..."), JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		try
		{
			// Check for encrypted wallet
			final boolean bEncryptedWallet = this.clientCaller.isWalletEncrypted();
			if (bEncryptedWallet)
			{
				PasswordDialog pd = new PasswordDialog((JFrame)(this.parent));
				pd.setVisible(true);
				
				if (!pd.isOKPressed())
				{
					return;
				}
				
				this.clientCaller.unlockWallet(pd.getPassword());
			}
			
			// TODO: We need a much more precise criterion to distinguish T/Z adresses;
			boolean isZAddress = address.startsWith("z") && address.length() > 40;
			
			String privateKey = isZAddress ?
				this.clientCaller.getZPrivateKey(address) : this.clientCaller.getTPrivateKey(address);
				
			// Lock the wallet again 
			if (bEncryptedWallet)
			{
				this.clientCaller.lockWallet();
			}
				
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(privateKey), null);
			
			JOptionPane.showMessageDialog(
				this.parent, 
				(isZAddress ? rb.S("Private (z)") : rb.S("Transparent (k1,jz)")) +  rb.S(" address:\n") +
				address + "\n" + 
				rb.S("has private key:\n") +
				privateKey + "\n\n" +
				rb.S("The private key has also been copied to the clipboard."), 
				rb.S("Private key information"), JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception ex)
		{
			this.errorReporter.reportError(ex, false);
		}
	}
	
	
	public void importSinglePrivateKey()
	{
		try
		{
			SingleKeyImportDialog kd = new SingleKeyImportDialog(this.parent, this.clientCaller);
			kd.setVisible(true);
			
		} catch (Exception ex)
		{
			this.errorReporter.reportError(ex, false);
		}
	}
	
	
	private void issueBackupDirectoryWarning()
		throws IOException
	{
        String userDir = OSUtil.getSettingsDirectory();
        File warningFlagFile = new File(userDir + File.separator + "backupInfoShown.flag");
        if (warningFlagFile.exists())
        {
            return;
        } else
        {
            warningFlagFile.createNewFile();
        }
        
        JOptionPane.showMessageDialog(
            this.parent,
            rb.S("For security reasons the wallet may be backed up/private keys exported only if\n") +
            rb.S("the kotod parameter -exportdir=<dir> has been set. If you started kotod \n") +
            rb.S("manually, you ought to have provided this parameter. When kotod is started \n") +
            rb.S("automatically by the GUI wallet the directory provided as parameter to -exportdir\n") +
            rb.S("is the user home directory: ") + OSUtil.getUserHomeDirectory().getCanonicalPath() +"\n" +
            rb.S("Please navigate to the directory provided as -exportdir=<dir> and select a\n") + 
            rb.S("filename in it to backup/export private keys. If you select another directory\n") +
            rb.S("instead, the destination file will still end up in the directory provided as \n") +
            rb.S("-exportdir=<dir>. If this parameter was not provided to kotod, the process\n") +
            rb.S("will fail with a security check error. The filename needs to consist of only\n") + 
            rb.S("alphanumeric characters (e.g. dot is not allowed).\n\n") +
            rb.S("(This message will be shown only once)"),
            rb.S("Wallet backup directory information"), JOptionPane.INFORMATION_MESSAGE);
	}
}
