/**
 * Copyright (c) 2018 by Visuality Systems, Ltd.
 * This file is a part of jNQ project. 
 */

package com.visuality.samples.browsecommandline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Vector;

import com.visuality.nq.auth.PasswordCredentials;
import com.visuality.nq.client.Directory;
import com.visuality.nq.client.Mount;
import com.visuality.nq.client.Network;
import com.visuality.nq.client.Share;
import com.visuality.nq.client.SmbInputStream;
import com.visuality.nq.client.SmbOutputStream;
import com.visuality.nq.common.NqException;
import com.visuality.nq.netbios.NetbiosDaemon;

/**
 * This class is a sample application utilizing jNQ API.
 * 
 * The jNQ API calls are marked with "// ++jNQ" comments
 */
public abstract class BrowseCommandLine {

	/**
	 * Select an object from a list of objects.
	 * 
	 * The objects are displayed through toString(). The list shows objects
	 * prefix with 1-based an entity number. Then, user is prompted to enter an
	 * entity number. A zero value means no selection.
	 *
	 * @param title
	 *            the title of the list
	 * @param objects
	 *            the objects to select from
	 * @return the selected object or <i>null</i> if the entered number iz zero
	 *         or it is above the object indexes.
	 * @throws NumberFormatException
	 *             The value entered is not a number
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private static Object selectObject(String title, Vector objects)
			throws NumberFormatException, IOException {
		System.out.println("");
		System.out.println("  " + title);

		for (int i = 1; i <= objects.size(); i++) {
			System.out.println("    " + i + " " + objects.get(i - 1));
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		int selector = Integer.parseInt(promptInput("Type selection"));
		if (0 == selector || selector > objects.size())
			return null;
		return objects.get(selector - 1);
	}

	/**
	 * Prompted text input.
	 *
	 * @param title
	 *            the prompt to display
	 * @return the typed string
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private static String promptInput(String title) throws IOException {
		System.out.println("  " + title + ":");
		return new BufferedReader(new InputStreamReader(System.in)).readLine();
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws Exception
	 *             the exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("");
		System.out
				.println("   *******************************************************");
		System.out
				.println("   **             Visuality Systems Ltd.                **");
		System.out
				.println("   **    Sample Application - jNQ Client Example        **");
		System.out
				.println("   *******************************************************");
		System.out
				.println("   *  This application creates a text file               *");
		System.out
				.println("   *  on a remote computer. User is advised to discover  *");
		System.out
				.println("   *  network configuration by browsing domains, hosts,  *");
		System.out
				.println("   *  shares and folders. Finally, user select a         *");
		System.out
				.println("   *  location for creating the file.                    *");
		System.out
				.println("   *  NOTE: User should be granted write access to the   *");
		System.out
				.println("   *        destination share and directory              *");
		System.out
				.println("   *******************************************************");
		try {
			NetbiosDaemon.start();

			/*
			 * Enumerate domains as Anonymous
			 */
			System.out.println("");
			System.out.println("  Discovering DOMAINS in the network...");
			Iterator it = null;
			try {
				it = Network.enumerateDomains(); // ++jNQ
			} finally {
			}
			if (null == it) {
				throw new Exception(
						"Network.enumerateDomains() returned null - unexpected"); 
			}
			System.out.println("");
			System.out.println("  List of domains:");
			Vector domains = new Vector();
			while (it.hasNext()) {
				domains.add((String) it.next());
			}
			String selectedDomain = (String) selectObject(
					"Select a Domain/Workgroup", domains);
			if (null == selectedDomain)
				throw new Exception("Illegal selection");
			System.out.println("  Domain '" + selectedDomain + "' chosen");

			/*
			 * Enumerate servers as Anonymous
			 */
			System.out.println("");
			System.out.println("  Discovering SERVERS in '" + selectedDomain
					+ "'...");
			it = null;
			try {
				it = Network.enumerateServers(selectedDomain);	// ++jNQ
			} finally {
			}
			if (null == it) {
				throw new Exception(
						"Network.enumerateServers() returned null - unexpected");
			}
			System.out.println("");
			System.out
					.println("  List of servers in '" + selectedDomain + "':");
			Vector servers = new Vector();
			while (it.hasNext()) {
				servers.add((String) it.next());
			}
			String selectedServer = (String) selectObject("Select a host",
					servers);
			if (null == selectedServer)
				throw new Exception("Illegal selection");
			System.out.println("  Host '" + selectedServer + "' chosen");

			/*
			 * Prompt for credentials since from now and on we cannot always be
			 * "anonymous"
			 */
			System.out.println("");
			System.out.println("  Enter credentials for host '"
					+ selectedServer + "'");
			String userName = promptInput("User name (for anonymous user enter '0')");
			String password = promptInput("Password for '" + userName + "'");
			String domain = promptInput("Domain '" + userName + "' belongs to");
			PasswordCredentials creds = userName.charAt(0) == '0' ? new PasswordCredentials(
					"", "", "") : new PasswordCredentials(userName, password,
					domain);

			/*
			 * Enumerate shares on server
			 */
			System.out.println("");
			System.out.println("  Discovering SHARES on '" + selectedServer
					+ "'...");
			it = null;
			try {
				it = Network.enumerateShares(selectedServer, creds);	// ++jNQ
			} finally {
			}
			if (null == it) {
				throw new Exception("Network.enumerateShares() 1"
						+ "returned null");
			}
			System.out.println("");
			System.out.println("  List of shares on '" + selectedServer + "':");
			Vector shares = new Vector();
			while (it.hasNext()) {
				shares.add(it.next());
			}
			Share.Info selectedShare = (Share.Info) selectObject(
					"Select a share", shares);
			if (null == selectedShare)
				throw new Exception("Illegal selection");
			System.out.println("  Share '" + selectedShare + "' chosen");

			/*
			 * Mount the selected share
			 */
			Mount mp = new Mount(selectedServer, selectedShare.name, creds);	// ++jNQ

			/*
			 * Traverse the tree recursively
			 */
			traverse(mp, "");

			Thread.sleep(200);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		System.exit(0);
	}

	/**
	 * Recursively traverse a folder.
	 *
	 * @param mp
	 *            the mount point
	 * @param folder
	 *            the folder to traverse
	 * @throws NqException
	 *             on any network error
	 * @throws NumberFormatException
	 *             the number format exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private static void traverse(Mount mp, String folder) throws NqException,
			NumberFormatException, IOException {
		/*
		 * Scan the directory, display only the child directories and allow to
		 * select one. An illegal input (e.g., - '0') will force creating a file
		 * in the current directory,
		 */
		Directory dir = new Directory(mp, folder);	// ++jNQ
		Directory.Entry entry;
		Vector folders = new Vector();
		while (null != (entry = dir.next())) {	// ++jNQ
			if (entry.info.isDirectory())
				folders.add(entry);
		}
		System.out.println("  Child folders in '\\" + folder + "'");
		entry = (Directory.Entry) selectObject(
				"Select a folder or type '0' to stop here", folders);
		if (null != entry) {
			traverse(mp, folder + ("".equals(folder) ? "" : "\\") + entry.name);
		}

		String fileName = folder + folder + ("".equals(folder) ? "" : "\\")
				+ "Test.txt";
		System.out.println("");
		System.out.print("  Creating file '" + fileName + "'...");
		PrintStream out = new PrintStream(new SmbOutputStream(mp, fileName));	// ++jNQ
		out.println("jNQ test");
		out.println("  jNQ test");
		out.println("    jNQ test");
		System.out.println("Done");
		out.close();
		System.out.println("  Reading back from '" + fileName + "'");
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new SmbInputStream(mp, fileName)));
		String text;
		while (null != (text = in.readLine())) {
			System.out.println("  " + text);
		}
		System.out.println("Done");
		System.out.println("");
		System.out.println("***** Test complete ******");
	}
}
