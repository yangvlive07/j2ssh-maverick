/**
 * Copyright 2003-2016 SSHTOOLS Limited. All Rights Reserved.
 *
 * For product documentation visit https://www.sshtools.com/
 *
 * This file is part of J2SSH Maverick.
 *
 * J2SSH Maverick is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * J2SSH Maverick is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with J2SSH Maverick.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.sshtools.net.SocketTransport;
import com.sshtools.publickey.SshPrivateKeyFile;
import com.sshtools.publickey.SshPrivateKeyFileFactory;
import com.sshtools.ssh.HostKeyVerification;
import com.sshtools.ssh.SshAuthentication;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshConnector;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh.SshSession;
import com.sshtools.ssh.components.SshKeyPair;
import com.sshtools.ssh.components.SshPublicKey;
import com.sshtools.ssh2.Ssh2HostbasedAuthentication;

/**
 * This example demonstrates how to connect using SSH2 hostbased authentication.
 * 
 * @author Lee David Painter
 */
public class SSH2HostbasedConnect {

	public static void main(String[] args) {

		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));

		try {

			System.out.print("Hostname: ");
			String hostname = reader.readLine();

			int idx = hostname.indexOf(':');
			int port = 22;
			if (idx > -1) {
				port = Integer.parseInt(hostname.substring(idx + 1));
				hostname = hostname.substring(0, idx);

			}

			System.out.print("Username [Enter for "
					+ System.getProperty("user.name") + "]: ");
			String username = reader.readLine();

			if (username == null || username.trim().equals(""))
				username = System.getProperty("user.name");

			System.out.println("Connecting to " + hostname);

			/**
			 * Create an SshConnector instance
			 */
			SshConnector con = SshConnector.createInstance();

			// Lets do some host key verification
			HostKeyVerification hkv = new HostKeyVerification() {
				public boolean verifyHost(String hostname, SshPublicKey key) {
					try {
						System.out.println("The connected host's key ("
								+ key.getAlgorithm() + ") is");
						System.out.println(key.getFingerprint());
					} catch (SshException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return true;
				}
			};

			con.getContext().setHostKeyVerification(hkv);

			/**
			 * Connect to the host
			 */
			SshClient ssh = con.connect(new SocketTransport(hostname, port),
					username, true);

			/**
			 * Authenticate the user using password authentication
			 */
			Ssh2HostbasedAuthentication pk = new Ssh2HostbasedAuthentication();

			do {
				pk.setClientUsername(username);
				// TODO This is 1.4 only - what effect will this have?

				// pk.setClientHostname(java.net.InetAddress.getLocalHost().getCanonicalHostName());
				pk.setClientHostname(java.net.InetAddress.getLocalHost()
						.getHostName());
				System.out.print("Private key file: ");
				SshPrivateKeyFile pkfile = SshPrivateKeyFileFactory
						.parse(new FileInputStream(
								"C:/Documents and Settings/lee/.ssh/id_rsa"));

				SshKeyPair pair;
				if (pkfile.isPassphraseProtected()) {
					System.out.print("Passphrase: ");
					pair = pkfile.toKeyPair(reader.readLine());
				} else
					pair = pkfile.toKeyPair(null);

				pk.setPrivateKey(pair.getPrivateKey());
				pk.setPublicKey(pair.getPublicKey());
			} while (ssh.authenticate(pk) != SshAuthentication.COMPLETE
					&& ssh.isConnected());

			/**
			 * Start a session and do basic IO
			 */
			if (ssh.isAuthenticated()) {

				SshSession session = ssh.openSessionChannel();
				session.requestPseudoTerminal("vt100", 80, 24, 0, 0);
				session.startShell();

				final InputStream in = session.getInputStream();
				Thread t = new Thread(new Runnable() {
					public void run() {
						try {
							int read;
							while ((read = in.read()) > -1) {
								if (read > 0)
									System.out.print((char) read);
							}
						} catch (Throwable t1) {
							t1.printStackTrace();
						} finally {
							System.exit(0);
						}
					}

				});
				t.start();
				int read;
				while ((read = System.in.read()) > -1) {
					session.getOutputStream().write(read);
				}

				System.exit(0);
			}

		} catch (Throwable th) {
			th.printStackTrace();
		}
	}

}
