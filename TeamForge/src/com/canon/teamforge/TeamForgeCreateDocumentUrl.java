package com.canon.teamforge;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.canon.teamforge.util.TeamForgeUtilCL;
import com.collabnet.ce.soap60.webservices.cemain.AssociationSoapRow;
import com.collabnet.ce.soap60.webservices.cemain.ProjectSoapRow;
import com.collabnet.ce.soap60.webservices.docman.DocumentFolderSoapRow;
import com.collabnet.ce.soap60.webservices.docman.DocumentSoapRow;
import com.collabnet.ce.soap60.webservices.tracker.Artifact2SoapDO;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import com.jcraft.jsch.JSchException;

public class TeamForgeCreateDocumentUrl
{
	public static void main(String args[])
	{
		System.out.println("TeamForgeCreateDocumentUrl v1.0.0 - Author: Marjo Hartman (DXC Technology)\n");
		int rc = 1;

		if (args.length < 7)
		{
			System.out.println("Usage: TeamForgeCreateDocumentUrl propfile application changeNumber title description url artifactId");
		}
		else
		{
			try
			{
				java.util.Properties props = new java.util.Properties();
				java.io.FileInputStream in = new java.io.FileInputStream(args[0]);
				props.load(in);
				in.close();

				String sUrl = props.getProperty("teamforge.url");
				String sUser = props.getProperty("teamforge.username");
				String sPass = props.getProperty("teamforge.password");
				String sProject = props.getProperty("teamforge.project");
				String sApplication = args[1];
				String sChangeNr = args[2];
				String sTitle = args[3];
				String sDescription = args[4];
				String sDocUrl = args[5];
				String sArtifactId = args[6];
				String status;

				TeamForgeUtilCL tf = new TeamForgeUtilCL(sUrl, sUser, sPass);
				FileWriter fileWriter = null;
				if (tf.login())
				{
					System.out.println("\nConnected to " + sUrl + " as " + sUser);
					
					ProjectSoapRow proj = tf.findProject(sProject);

					if (null == proj)
					{
						System.out.println("Project " + sProject + " does not exist\n");
						System.out.println("Please correct TeamForge.properties and restart\n");
					}
					else
					{
						DocumentFolderSoapRow[] dfsrArr = tf.getDocumentFolderList(proj.getId());
						String rootFolderId = dfsrArr[0].getId();
						dfsrArr = tf.getDocumentFolderList(rootFolderId);
						String appFolderId = null;

						for (int i = 0; i < dfsrArr.length; ++i)
						{
							if (dfsrArr[i].getTitle().equals(sApplication))
							{
								appFolderId = dfsrArr[i].getId();
								break;
							}
						}

						if (null == appFolderId)
						{
							appFolderId = tf.createDocumentFolder(rootFolderId, sApplication, sApplication + " - main folder");
						}

						if (null != appFolderId)
						{
							dfsrArr = tf.getDocumentFolderList(appFolderId);
							String changeFolderId = null;

							for (int i = 0; i < dfsrArr.length; ++i)
							{
								if (dfsrArr[i].getTitle().equals(sChangeNr))
								{
									changeFolderId = dfsrArr[i].getId();
									break;
								}
							}

							if (null == changeFolderId)
							{
								changeFolderId = tf.createDocumentFolder(appFolderId, sChangeNr, sChangeNr + " - change folder");
							}

							if (null != changeFolderId)
							{
								DocumentSoapRow dsr = tf.getDocument(changeFolderId, sTitle);
								
								if (null != dsr)
								{
									//tf.deleteDocument(dsr.getId());
									System.out.println("Deleted document " + dsr.getId());
								}
								System.out.println("before");
								
								//tf.createDocumentWithUrl(changeFolderId, sTitle, sDescription, sDocUrl, sArtifactId);
								//System.out.println("Created document " + sTitle + " with url " +  sDocUrl + " below " + sArtifactId);
								
								//Document URL
								String docURL = dsr.getFileUrl();
								System.out.println("Document URL--------->"+docURL);
								
								//Status
								Artifact2SoapDO artifact2SoapDO=tf.getArtifactData("artf91041");
								String artifactStatus=artifact2SoapDO.getStatus();
								System.out.println("status--------->" +artifactStatus);
								fileWriter = new FileWriter("D:/sample.csv");
								Object[] dynamicFields = new String[2];
								Object[] dynamicValues = new Object[2];
								dynamicFields[0]=new String("artifactStatus");
								dynamicFields[1]=new String("docURL");
								dynamicValues[0]=artifactStatus;
								dynamicValues[1]=docURL;
								CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT.withHeaderComments(dynamicFields));
								 //new CSVPrinter(
								csvPrinter.printRecord(dynamicValues);
								csvPrinter.flush();
								csvPrinter.close();
							//	connectionToSSH(docURL, artifactStatus);
								
								/*Properties prop = new Properties();
								try {
									prop.setProperty("status", artifact2SoapDO.getStatus());
									prop.setProperty("statustype", artifact2SoapDO.getStatusClass());
									prop.store(new FileOutputStream("D:\\jenkins/status.properties"), null);
								} catch (IOException ex) {
									ex.printStackTrace();
								}*/
								
								/*status=artifact2SoapDO.getStatus();
								PrintStream originalOut = System.out;
								originalOut = new PrintStream(new FileOutputStream("D:\\jenkins/output.txt"));
								originalOut.print(status);*/
								rc = 0;
							}
						}
					}

					tf.logoff();
					System.out.println("Disconnected from " + sUrl);
					
				}
			}
			catch (java.io.IOException e)
			{
				System.out.println("IO Exception: " + e.getMessage());
				e.printStackTrace();
			}
		}

		System.exit(rc);
	}
}
