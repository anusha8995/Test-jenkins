package com.canon.teamforge.util;

import com.collabnet.ce.soap60.webservices.ClientSoapStubFactory;
import com.collabnet.ce.soap60.webservices.cemain.ICollabNetSoap;
import com.collabnet.ce.soap60.webservices.docman.IDocumentAppSoap;
import com.collabnet.ce.soap60.webservices.planning.IPlanningAppSoap;
import com.collabnet.ce.soap60.webservices.tracker.ITrackerAppSoap;
import com.collabnet.ce.soap60.webservices.scm.IScmAppSoap;
import com.collabnet.ce.soap60.webservices.filestorage.IFileStorageAppSoap;
import com.collabnet.ce.soap60.webservices.cemain.UserSoapDO;
import com.collabnet.ce.soap60.webservices.cemain.ProjectSoapRow;
import com.collabnet.ce.soap60.webservices.cemain.ProjectSoapList;
import com.collabnet.ce.soap60.webservices.cemain.TrackerFieldSoapDO;
import com.collabnet.ce.soap60.webservices.docman.DocumentFolderSoapList;
import com.collabnet.ce.soap60.webservices.docman.DocumentFolderSoapRow;
import com.collabnet.ce.soap60.webservices.docman.DocumentFolderSoapDO;
import com.collabnet.ce.soap60.webservices.docman.DocumentSoapList;
import com.collabnet.ce.soap60.webservices.docman.DocumentSoapRow;
import com.collabnet.ce.soap60.webservices.planning.PlanningFolder4SoapDO;
import com.collabnet.ce.soap60.webservices.planning.PlanningFolder4SoapRow;
import com.collabnet.ce.soap60.webservices.planning.PlanningFolder4SoapList;
import com.collabnet.ce.soap60.webservices.planning.ArtifactsInPlanningFolderSoapRow;
import com.collabnet.ce.soap60.webservices.planning.ArtifactsInPlanningFolderSoapList;
import com.collabnet.ce.soap60.webservices.tracker.Tracker3SoapDO;
import com.collabnet.ce.soap60.webservices.tracker.Tracker3SoapRow;
import com.collabnet.ce.soap60.webservices.tracker.Tracker3SoapList;
import com.collabnet.ce.soap60.webservices.tracker.TrackerFieldValueSoapDO;
import com.collabnet.ce.soap60.webservices.tracker.OrderedTrackerFieldSoapRow;
import com.collabnet.ce.soap60.webservices.tracker.OrderedTrackerFieldSoapList;
import com.collabnet.ce.soap60.webservices.tracker.ArtifactDependencySoapRow;
import com.collabnet.ce.soap60.webservices.tracker.ArtifactDependencySoapList;
import com.collabnet.ce.soap60.webservices.tracker.ArtifactDetail2SoapRow;
import com.collabnet.ce.soap60.webservices.tracker.ArtifactDetail2SoapList;
import com.collabnet.ce.soap60.webservices.tracker.Artifact2SoapDO;
import com.collabnet.ce.soap60.webservices.tracker.Artifact2SoapList;
import com.collabnet.ce.soap60.webservices.tracker.Artifact2SoapRow;
import com.collabnet.ce.soap60.webservices.cemain.AuditHistorySoapList;
import com.collabnet.ce.soap60.webservices.cemain.AuditHistorySoapRow;
import com.collabnet.ce.soap60.webservices.cemain.AssociationSoapList;
import com.collabnet.ce.soap60.webservices.cemain.AssociationSoapRow;
import com.collabnet.ce.soap60.webservices.cemain.AttachmentSoapDO;
import com.collabnet.ce.soap60.webservices.cemain.CommentSoapList;
import com.collabnet.ce.soap60.webservices.cemain.CommentSoapRow;
import com.collabnet.ce.soap60.webservices.scm.RepositorySoapDO;
import com.collabnet.ce.soap60.webservices.scm.Commit2SoapDO;
import com.collabnet.ce.soap60.types.SoapFilter;
import com.collabnet.ce.soap60.types.SoapFieldValues;

public abstract class TeamForgeUtil
{
	private final int NUM_RETRIES = 3;
	private ICollabNetSoap m_sfSoap;
	private IDocumentAppSoap m_docSoap;
	private IPlanningAppSoap m_planningSoap;
	private ITrackerAppSoap m_trackerSoap;
	private IScmAppSoap m_scmSoap;
	private IFileStorageAppSoap m_filestorageSoap;
	private String m_sUrl;
	private String m_sUser;
	private String m_sPass;
	private String m_sSessionId;
	private int m_iRetry;
	private java.util.Map<String, PlanningFolder4SoapDO> m_folders = new java.util.TreeMap<String, PlanningFolder4SoapDO>();

	public TeamForgeUtil(String sUrl, String sUser, String sPass)
	{
		m_sUrl = sUrl;
		m_sUser = sUser;
		m_sPass = sPass;
		m_sfSoap = (ICollabNetSoap) ClientSoapStubFactory.getSoapStub(ICollabNetSoap.class, m_sUrl, 600000);
		m_docSoap = (IDocumentAppSoap) ClientSoapStubFactory.getSoapStub(IDocumentAppSoap.class, m_sUrl, 600000);
		m_planningSoap = (IPlanningAppSoap) ClientSoapStubFactory.getSoapStub(IPlanningAppSoap.class, m_sUrl, 600000);
		m_trackerSoap = (ITrackerAppSoap) ClientSoapStubFactory.getSoapStub(ITrackerAppSoap.class, m_sUrl, 600000);
		m_scmSoap = (IScmAppSoap) ClientSoapStubFactory.getSoapStub(IScmAppSoap.class, m_sUrl, 600000);
		m_filestorageSoap = (IFileStorageAppSoap) ClientSoapStubFactory.getSoapStub(IFileStorageAppSoap.class, m_sUrl, 600000);
		m_iRetry = NUM_RETRIES;
	}

	abstract void errorMsg(String sMsg);

	abstract void errorMsg(String sMsg, Throwable t);

	private Boolean retryAfterException(String sMsg)
	{
		Boolean fRetry = (m_iRetry > 0)	&&
						 (sMsg.contains("Connection timed out") ||
						  sMsg.contains("connection abort") ||
						  sMsg.contains("Connection reset") ||
						  sMsg.contains("UnknownHostException")) &&
						 login();
		if (fRetry)
		{
			--m_iRetry;
		}

		return fRetry;
	}

	public Boolean login()
	{
		Boolean fGoOn = true;

		try
		{
			m_sSessionId = m_sfSoap.login(m_sUser, m_sPass);
		}
		catch (java.rmi.RemoteException e)
		{
			errorMsg("Unable to connect to TeamForge: " + e.getMessage(), e);
			fGoOn = false;
		}

		return fGoOn;
	}

	public void logoff()
	{
		try
		{
			// m_sfSoap.keepAlive(m_sSessionId);
			m_sfSoap.logoff(m_sUser, m_sSessionId);
		}
		catch (java.rmi.RemoteException e)
		{
			errorMsg("Unable to disconnect from TeamForge: " + e.getMessage(), e);
		}
	}

	public UserSoapDO getUser(String fullName)
	{
		UserSoapDO usd = null;

		try
		{
			usd = m_sfSoap.getUserByName(m_sSessionId, fullName);
			m_iRetry = NUM_RETRIES;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getUser(fullName);
			}
			else
			{
				errorMsg("Unable to retrieve user data for " + fullName + ": " + sMsg, e);
			}
		}

		return (usd);
	}

	public ProjectSoapRow findProject(String projectName)
	{
		ProjectSoapRow psr = null;

		try
		{
			ProjectSoapList psl = m_sfSoap.findProjects(m_sSessionId, "Title = \"" + projectName + "\"");

			if (psl.getDataRows().length > 0)
			{
				psr = psl.getDataRows()[0];
			}

			m_iRetry = NUM_RETRIES;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return findProject(projectName);
			}
			else
			{
				errorMsg("Unable to find project " + projectName + ": " + sMsg, e);
			}
		}

		return psr;
	}

	public java.util.List<PlanningFolder4SoapRow> getPlanningFolderList(String parentId, boolean recurse)
	{
		try
		{
			PlanningFolder4SoapList psl = m_planningSoap.getPlanningFolder4List(m_sSessionId, parentId, recurse);
			PlanningFolder4SoapRow[] projRows = psl.getDataRows();
			java.util.List<PlanningFolder4SoapRow> planningFolders = new java.util.ArrayList<PlanningFolder4SoapRow>();

			// Return planning folders in descending order
			for (int i = projRows.length; i > 0; --i)
			{
				planningFolders.add(projRows[i - 1]);
			}

			m_iRetry = NUM_RETRIES;
			return planningFolders;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getPlanningFolderList(parentId, recurse);
			}
			else
			{
				errorMsg("Unable to retrieve planningFolders: " + sMsg, e);
			}
		}

		return null;
	}

	public String getPlanningFolderId(java.util.List<PlanningFolder4SoapRow> listPlanningFolders, String folderName)
	{
		String folderId = null;

		for (PlanningFolder4SoapRow row : listPlanningFolders)
		{
			if (row.getTitle().equals(folderName))
			{
				folderId = row.getId();
			}
		}

		return folderId;
	}

	public ArtifactDetail2SoapRow[] getArtifactDetailList(String projectId, String trackerId, java.util.TreeMap<String, String> planningFolders)
	{
		return getArtifactDetailList(projectId, trackerId, planningFolders,	null, null, null);
	}

	public ArtifactDetail2SoapRow[] getArtifactDetailList(
		String projectId,
		String trackerId,
		java.util.TreeMap<String, String> planningFolders,
		java.util.List<String> statusses,
		java.util.List<String> environments,
		java.util.List<String> applications)
	{
		try
		{
			SoapFilter[] filters = null;
			int iSize = (null == planningFolders ? 0 : planningFolders.size()) +
						(null == statusses ? 0 : statusses.size()) +
						(null == environments ? 0 : environments.size()) +
						(null == applications ? 0 : applications.size());

			if (0 < iSize)
			{
				filters = new SoapFilter[iSize];
				int i = 0;

				if (null != planningFolders)
				{
					for (java.util.Map.Entry<String, String> pf : planningFolders.entrySet())
					{
						filters[i++] = new SoapFilter(Artifact2SoapDO.COLUMN_PLANNING_FOLDER_ID, pf.getKey());
					}
				}

				if (null != statusses)
				{
					for (String s : statusses)
					{
						filters[i++] = new SoapFilter(Artifact2SoapDO.COLUMN_STATUS, s);
					}
				}

				if (null != environments)
				{
					for (String s : environments)
					{
						filters[i++] = new SoapFilter("Environment", s);
					}
				}

				if (null != applications)
				{
					for (String s : applications)
					{
						filters[i++] = new SoapFilter("Application", s);
					}
				}
			}

			ArtifactDetail2SoapList adsl = m_trackerSoap.getArtifactDetailList2(m_sSessionId, trackerId, null, filters, null, 0, -1, false, true);
			m_iRetry = NUM_RETRIES;
			return adsl.getDataRows();
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getArtifactDetailList(projectId, trackerId, planningFolders, statusses, environments, applications);
			}
			else
			{
				errorMsg("Unable to retrieve artifacts for planning folders: " + sMsg, e);
			}
		}

		return null;
	}
	
	public ArtifactDetail2SoapRow[] getArtifactDetailListChangeId(String projectId, String trackerId, java.util.List<String> changeIds)
	{
		try
		{
			SoapFilter[] filters = null;
			int iSize = (null == changeIds ? 0 : changeIds.size());

			if (0 < iSize)
			{
				filters = new SoapFilter[iSize];
				int i = 0;

				if (null != changeIds)
				{
					for (String s : changeIds)
					{
						filters[i++] = new SoapFilter("Change ID", s);
					}
				}
			}

			ArtifactDetail2SoapList adsl = m_trackerSoap.getArtifactDetailList2(m_sSessionId, trackerId, null, filters, null, 0, -1, false, true);
			m_iRetry = NUM_RETRIES;
			return adsl.getDataRows();
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getArtifactDetailListChangeId(projectId, trackerId, changeIds);
			}
			else
			{
				errorMsg("Unable to retrieve artifacts for change ids: " + sMsg, e);
			}
		}

		return null;
	}

	public ArtifactDetail2SoapRow[] getArtifactDetailListCategory(
		String projectId,
		String trackerId,
		java.util.TreeMap<String, String> planningFolders,
		java.util.List<String> statusses,
		java.util.List<String> environments,
		java.util.List<String> applications,
		java.util.List<String> category)
	{
		try
		{
			SoapFilter[] filters = null;
			int iSize = (null == planningFolders ? 0 : planningFolders.size()) +
						(null == statusses ? 0 : statusses.size()) +
						(null == environments ? 0 : environments.size()) +
						(null == applications ? 0 : applications.size()) +
						(null == category ? 0 : category.size());

			if (0 < iSize)
			{
				filters = new SoapFilter[iSize];
				int i = 0;

				if (null != planningFolders)
				{
					for (java.util.Map.Entry<String, String> pf : planningFolders.entrySet())
					{
						filters[i++] = new SoapFilter(Artifact2SoapDO.COLUMN_PLANNING_FOLDER_ID, pf.getKey());
					}
				}

				if (null != statusses)
				{
					for (String s : statusses)
					{
						filters[i++] = new SoapFilter(Artifact2SoapDO.COLUMN_STATUS, s);
					}
				}

				if (null != environments)
				{
					for (String s : environments)
					{
						filters[i++] = new SoapFilter("Environment", s);
					}
				}

				if (null != applications)
				{
					for (String s : applications)
					{
						filters[i++] = new SoapFilter("Application", s);
					}
				}
				
				if (null != category)
				{
					for (String s : category)
					{
						filters[i++] = new SoapFilter(Artifact2SoapDO.FILTER_CATEGORY, s);
					}
				}
			}

			ArtifactDetail2SoapList adsl = m_trackerSoap.getArtifactDetailList2(m_sSessionId, trackerId, null, filters, null, 0, -1, false, true);
			m_iRetry = NUM_RETRIES;
			return adsl.getDataRows();
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getArtifactDetailListCategory(projectId, trackerId, planningFolders, statusses, environments, applications, category);
			}
			else
			{
				errorMsg("Unable to retrieve artifacts for planning folders: " + sMsg, e);
			}
		}

		return null;
	}

	public java.util.List<Tracker3SoapRow> getTrackerList(String projectId)
	{
		try
		{
			Tracker3SoapList tsl = m_trackerSoap.getTracker3List(m_sSessionId, projectId);
			Tracker3SoapRow[] tRows = tsl.getDataRows();
			java.util.List<Tracker3SoapRow> trackers = new java.util.ArrayList<Tracker3SoapRow>();
			trackers.addAll(java.util.Arrays.asList(tRows));
			m_iRetry = NUM_RETRIES;
			return (trackers);
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getTrackerList(projectId);
			}
			else
			{
				errorMsg("Unable to retrieve trackers for " + projectId + ": " + sMsg, e);
			}
		}

		return null;
	}

	public TrackerFieldSoapDO[] getTrackerFields(String trackerId)
	{
		try
		{
			TrackerFieldSoapDO[] tfs = m_trackerSoap.getFields(m_sSessionId, trackerId);
			m_iRetry = NUM_RETRIES;
			return (tfs);
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getTrackerFields(trackerId);
			}
			else
			{
				errorMsg("Unable to retrieve trackers fields for " + trackerId + ": " + sMsg, e);
			}
		}

		return null;
	}

	public String[] getStatusValues(String trackerId)
	{
		try
		{
			OrderedTrackerFieldSoapList otfsl = m_trackerSoap.getOrderedTrackerFields(m_sSessionId, trackerId);
			String[] fieldValues = null;

			for (OrderedTrackerFieldSoapRow tfRow : otfsl.getDataRows())
			{
				if (tfRow.getFieldName().equals("status"))
				{
					fieldValues = new String[tfRow.getFieldValues().length];
					int i = 0;

					for (TrackerFieldValueSoapDO fv : tfRow.getFieldValues())
					{
						fieldValues[i++] = fv.getValue();
					}
				}
			}

			m_iRetry = NUM_RETRIES;
			return fieldValues;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getStatusValues(trackerId);
			}
			else
			{
				errorMsg("Unable to retrieve tracker fields: " + sMsg, e);
			}
		}

		return null;
	}

	public Artifact2SoapDO getArtifactData(String artifactId)
	{
		try
		{
			Artifact2SoapDO asd = m_trackerSoap.getArtifactData2(m_sSessionId, artifactId);
			m_iRetry = NUM_RETRIES;
			return asd;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getArtifactData(artifactId);
			}
			else
			{
				errorMsg("Unable to retrieve artifact data for " + artifactId + ": " + sMsg, e);
			}
		}

		return null;
	}
	
	public AuditHistorySoapRow[] getAuditHistoryList(String artifactId)
	{
		try
		{
			AuditHistorySoapList ahsl = m_sfSoap.getAuditHistoryList(m_sSessionId, artifactId, false);
			AuditHistorySoapRow[] ahsRows = ahsl.getDataRows();
			m_iRetry = NUM_RETRIES;
			return (ahsRows);
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getAuditHistoryList(artifactId);
			}
			else
			{
				errorMsg("Unable to retrieve audit history for " + artifactId + ": " + sMsg, e);
			}
		}

		return null;
	}

	public Tracker3SoapDO getTrackerData(String trackerId)
	{
		try
		{
			Tracker3SoapDO tsd = m_trackerSoap.getTracker3Data(m_sSessionId, trackerId);
			m_iRetry = NUM_RETRIES;
			return tsd;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getTrackerData(trackerId);
			}
			else
			{
				errorMsg("Unable to retrieve tracker data for " + trackerId + ": " + sMsg, e);
			}
		}

		return null;
	}

	public ArtifactDependencySoapRow[] getArtifactChildDependencyList(String artifactId)
	{
		try
		{
			ArtifactDependencySoapList adsl = m_trackerSoap.getChildDependencyList(m_sSessionId, artifactId);
			ArtifactDependencySoapRow[] artfDepRows = adsl.getDataRows();
			m_iRetry = NUM_RETRIES;
			return artfDepRows;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getArtifactChildDependencyList(artifactId);
			}
			else
			{
				errorMsg("Unable to retrieve child dependencies for artifact " + artifactId + ": " + sMsg, e);
			}
		}

		return null;
	}

	public AssociationSoapRow[] getAssociations(String artifactId)
	{
		try
		{
			AssociationSoapList asl = m_sfSoap.getAssociationList(m_sSessionId, artifactId);
			AssociationSoapRow[] assRows = asl.getDataRows();
			m_iRetry = NUM_RETRIES;
			return asl.getDataRows();
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getAssociations(artifactId);
			}
			else
			{
				errorMsg("Unable to retrieve associations for " + artifactId + ": " + sMsg, e);
			}
		}

		return null;
	}

	public Boolean deleteAssociation(String id1, String id2)
	{
		try
		{
			m_sfSoap.deleteAssociation(m_sSessionId, id1, id2);
			m_iRetry = NUM_RETRIES;
			return true;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return deleteAssociation(id1, id2);
			}
			else
			{
				errorMsg("Unable to delete association between " + id1 + " and " + id2 + ": " + sMsg, e);
			}
		}

		return false;
	}

	public Commit2SoapDO getCommitData(String commitId)
	{
		try
		{
			Commit2SoapDO csd = m_scmSoap.getCommitData2(m_sSessionId, commitId);
			m_iRetry = NUM_RETRIES;
			return csd;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getCommitData(commitId);
			}
			else
			{
				errorMsg("Unable to retrieve commit data for " + commitId + ": " + sMsg, e);
			}
		}

		return null;
	}

	public RepositorySoapDO getRepositoryData(String repositoryId)
	{
		try
		{
			RepositorySoapDO rsd = m_scmSoap.getRepositoryDataById(m_sSessionId, repositoryId);
			m_iRetry = NUM_RETRIES;
			return rsd;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getRepositoryData(repositoryId);
			}
			else
			{
				errorMsg("Unable to retrieve repository data for " + repositoryId + ": " + sMsg, e);
			}
		}

		return null;
	}

	public String getParent(String artifactId)
	{
		String parent = null;

		try
		{
			ArtifactDependencySoapList adsl = m_trackerSoap
					.getParentDependencyList(m_sSessionId, artifactId);

			if (adsl.getDataRows().length > 0)
			{
				parent = adsl.getDataRows()[0].getOriginId();
			}

			m_iRetry = NUM_RETRIES;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getParent(artifactId);
			}
			else
			{
				errorMsg("Unable to retrieve artifact dependencies for " + artifactId + ": " + sMsg, e);
			}
		}

		return parent;
	}

	public ArtifactDependencySoapRow getParentRow(String artifactId)
	{
		ArtifactDependencySoapRow parent = null;

		try
		{
			ArtifactDependencySoapList adsl = m_trackerSoap.getParentDependencyList(m_sSessionId, artifactId);

			if (adsl.getDataRows().length > 0)
			{
				parent = adsl.getDataRows()[0];
			}

			m_iRetry = NUM_RETRIES;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getParentRow(artifactId);
			}
			else
			{
				errorMsg("Unable to retrieve artifact dependencies for " + artifactId + ": " + sMsg, e);
			}
		}

		return parent;
	}

	public void setArtifactData(Artifact2SoapDO ado, String comment)
	{
		try
		{
			m_trackerSoap.setArtifactData3(m_sSessionId, ado, comment, null);
			m_iRetry = NUM_RETRIES;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				setArtifactData(ado, comment);
			}
			else
			{
				errorMsg("Unable to set artifact " + ado.getId() + ": " + sMsg, e);
			}
		}
	}

	public void setArtifactData(Artifact2SoapDO ado, String comment, String attachment, String fileId)
	{
		try
		{
			AttachmentSoapDO[] attachments = new AttachmentSoapDO[1];
			attachments[0] = new AttachmentSoapDO();
			attachments[0].setAttachmentFileId(fileId);
			attachments[0].setAttachmentFileName(attachment);
			attachments[0].setAttachmentMimeType("text/plain");
			m_trackerSoap.setArtifactData3(m_sSessionId, ado, comment, attachments);
			m_iRetry = NUM_RETRIES;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				setArtifactData(ado, comment, attachment, fileId);
			}
			else
			{
				errorMsg("Unable to set artifact " + ado.getId() + ": " + sMsg, e);
			}
		}
	}

	public String uploadFileWithId(String projectId, String folderId, String docFilePath)
	{
		String uploadId = null;
		java.io.File myFile = new java.io.File(docFilePath);

		if (!myFile.canRead())
		{
			errorMsg("Unable to read local file " + docFilePath);
		}
		else
		{
			javax.activation.DataSource ds = new javax.activation.FileDataSource(myFile);
			javax.activation.DataHandler dh = new javax.activation.DataHandler(ds);

			try
			{
				uploadId = m_filestorageSoap.uploadFile(m_sSessionId, dh);
				m_iRetry = NUM_RETRIES;
			}
			catch (java.rmi.RemoteException e)
			{
				String sMsg = e.getMessage();

				if (retryAfterException(sMsg))
				{
					return uploadFileWithId(projectId, folderId, docFilePath);
				}
				else
				{
					errorMsg("Unable to upload file " + docFilePath + ": " + sMsg, e);
				}
			}
		}

		return uploadId;
	}

	public CommentSoapRow[] getComments(String artifactId)
	{
		try
		{
			CommentSoapList csl = m_sfSoap.getCommentList(m_sSessionId, artifactId);
			CommentSoapRow[] csRows = csl.getDataRows();
			m_iRetry = NUM_RETRIES;
			return csl.getDataRows();
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getComments(artifactId);
			}
			else
			{
				errorMsg("Unable to retrieve comments for " + artifactId + ": " + sMsg, e);
			}
		}

		return null;
	}

	public Artifact2SoapDO createChangeArtifact(String trackerId,
		String planningFolderId,
		java.util.Map<String, String> artifactData)
	{
		try
		{
			java.util.ArrayList<String> customFieldNames = new java.util.ArrayList<String>();
			java.util.ArrayList<String> customFieldTypes = new java.util.ArrayList<String>();
			java.util.ArrayList<Object> customFieldValues = new java.util.ArrayList<Object>();

			customFieldNames.add("Change ID");
			customFieldTypes.add(TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING);
			customFieldValues.add("CHxxxxxx");

			customFieldNames.add("Creation Date");
			customFieldTypes.add(TrackerFieldSoapDO.FIELD_VALUE_TYPE_DATE);
			customFieldValues.add(new java.util.Date());

			for (java.util.Map.Entry<String, String> pair : artifactData.entrySet())
			{
				if (pair.getKey().equals("Solution Type") ||
					pair.getKey().equals("Stream Release Manager") ||
					pair.getKey().equals("Deployment Method") ||
					pair.getKey().equals("Domain") ||
					pair.getKey().equals("E2E Process") ||
					pair.getKey().equals("Sequence Number") ||
					pair.getKey().equals("Release") ||
					pair.getKey().equals("Pre Req (CH number)") ||
					pair.getKey().equals("CR Number") ||
					pair.getKey().equals("SR/Defect Number") ||
					pair.getKey().equals("Vendor Patch ID") ||
					pair.getKey().equals("Repository?") ||
					pair.getKey().equals("Technical Setup Required?") ||
					pair.getKey().equals("Functional Setup Required?") ||
					pair.getKey().equals("Functional Setup Team?") ||
					pair.getKey().equals("Post Install Right After?") ||
					pair.getKey().equals("Test QA TST") ||
					pair.getKey().equals("Test QA ACC") ||
					pair.getKey().equals("Work Package") ||
					pair.getKey().equals("Release Manager Notes") ||
					pair.getKey().equals("TAM Installation Remark") ||
					pair.getKey().equals("FAM/TAM Setup Remark"))
				{
					customFieldNames.add(pair.getKey());
					customFieldTypes.add(TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING);
					customFieldValues.add(pair.getValue());
				}
			}

			SoapFieldValues flexFields = new SoapFieldValues();
			flexFields.setNames(customFieldNames.toArray(new String[customFieldNames.size()])); 
			flexFields.setTypes(customFieldTypes.toArray(new String[customFieldTypes.size()])); 
			flexFields.setValues(customFieldValues.toArray(new Object[customFieldValues.size()]));

			Artifact2SoapDO asd = m_trackerSoap.createArtifact3(
				m_sSessionId,
				trackerId,
				artifactData.get("Title"),
				artifactData.get("Description"),
				null,
				artifactData.get("Category"),
				"Development",
				null,
				Integer.valueOf(artifactData.get("Priority")),
				0,
				0,
				false,
				0,
				false,
				artifactData.get("Assigned To"),
				null,
				planningFolderId,
				null,
				flexFields,
				null);
			m_iRetry = NUM_RETRIES;
			return asd;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return createChangeArtifact(
					trackerId,
					planningFolderId,
					artifactData);
			}
			else
			{
				errorMsg("Unable to create artifact for '" + artifactData.get("Title") + "': " + sMsg, e);
			}
		}

		return null;
	}

	public Artifact2SoapDO createInstallArtifact(String trackerId,
		String planningFolderId,
		java.util.Map<String, String> artifactData)
	{
		try
		{
			java.util.ArrayList<String> customFieldNames = new java.util.ArrayList<String>();
			java.util.ArrayList<String> customFieldTypes = new java.util.ArrayList<String>();
			java.util.ArrayList<Object> customFieldValues = new java.util.ArrayList<Object>();

			for (java.util.Map.Entry<String, String> pair : artifactData.entrySet())
			{
				if (pair.getKey().equals("Change Version") ||
					pair.getKey().equals("Application") ||
					pair.getKey().equals("Environment") ||
					pair.getKey().equals("Approved for Cont. Deployment") ||
					pair.getKey().equals("Installation Requirements") ||
					pair.getKey().equals("Automatically Installed?") ||
					pair.getKey().equals("Expedited Request?") ||
					pair.getKey().equals("Release Manager Notes") ||
					pair.getKey().equals("Installation SR") ||
					pair.getKey().equals("Refresh ID") ||
					pair.getKey().equals("Remarks"))
				{
					customFieldNames.add(pair.getKey());
					customFieldTypes.add(TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING);
					customFieldValues.add(pair.getValue());
				}
			}

			SoapFieldValues flexFields = new SoapFieldValues();
			flexFields.setNames(customFieldNames.toArray(new String[customFieldNames.size()])); 
			flexFields.setTypes(customFieldTypes.toArray(new String[customFieldTypes.size()])); 
			flexFields.setValues(customFieldValues.toArray(new Object[customFieldValues.size()]));

			Artifact2SoapDO asd = m_trackerSoap.createArtifact3(
				m_sSessionId,
				trackerId,
				artifactData.get("Title"),
				artifactData.get("Description"),
				null,
				artifactData.get("Category"),
				"Development",
				null,
				Integer.valueOf(artifactData.get("Priority")),
				0,
				0,
				false,
				0,
				false,
				artifactData.get("Assigned To"),
				null,
				planningFolderId,
				null,
				flexFields,
				null);
			m_iRetry = NUM_RETRIES;
			return asd;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return createInstallArtifact(
					trackerId,
					planningFolderId,
					artifactData);
			}
			else
			{
				errorMsg("Unable to create artifact for '" + artifactData.get("Title") + "': " + sMsg, e);
			}
		}

		return null;
	}

	public Artifact2SoapDO createDataMaskingArtifact(
		String trackerId,
		String title,
		String description,
		String category,
		String status,
		String assignedUsername,
		String planningFolderId,
		String application,
		String environment,
		int durationInMinutes)
	{
		try
		{
			java.text.SimpleDateFormat dt = new java.text.SimpleDateFormat("HH:mm");
			SoapFieldValues flexFields = new SoapFieldValues();
			flexFields.setNames(new String []
			{
				"Change Version",
				"Application",
				"Environment",
				"Automatically Installed?",
				"Installation Requirements",
				"Approved for Cont. Deployment",
				"Release",
				"Actual Install Date",
				"Time Installation Completed",
				"Installation Duration"
			}); 
			flexFields.setTypes(new String []
			{
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_DATE,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING
			}); 
			flexFields.setValues(new Object []
			{
				"V01",
				application,
				environment,
				"Yes",
				"Daytime Install with Backup",
				"No",
				"Data Masking",
				new java.util.Date(),
				new String(dt.format(new java.util.Date())),
				String.valueOf(durationInMinutes)
			});

			Artifact2SoapDO asd = m_trackerSoap.createArtifact3(
				m_sSessionId,
				trackerId,
				title,
				description,
				null,
				category,
				status,
				null,
				0,
				0,
				0,
				false,
				0,
				false,
				assignedUsername,
				null,
				planningFolderId,
				null,
				flexFields,
				null);
			m_iRetry = NUM_RETRIES;
			return asd;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return createDataMaskingArtifact(
					trackerId,
					title,
					description,
					category,
					status,
					assignedUsername,
					planningFolderId,
					application,
					environment,
					durationInMinutes);
			}
			else
			{
				errorMsg("Unable to create artifact for '" + title + "': " + sMsg, e);
			}
		}

		return null;
	}

	public Artifact2SoapDO createRefreshArtifact(
		String trackerId,
		String assignedUsername,
		String application,
		String sourceEnvironment,
		String targetEnvironment,
		java.util.Date refreshDate,
		String planningFolder)
	{
		String title = "Refresh " + application + " from " + sourceEnvironment + " to " + targetEnvironment;

		try
		{
			SoapFieldValues flexFields = new SoapFieldValues();
			flexFields.setNames(new String []
			{
				"Refresh ID",
				"Application",
				"Source Environment",
				"Target Environment",
				"Refresh Date"
			}); 
			flexFields.setTypes(new String []
			{
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_STRING,
				TrackerFieldSoapDO.FIELD_VALUE_TYPE_DATE
			}); 
			flexFields.setValues(new Object []
			{
				(new java.util.Date()).toString(),
				application,
				sourceEnvironment,
				targetEnvironment,
				refreshDate
			});

			Artifact2SoapDO asd = m_trackerSoap.createArtifact3(
				m_sSessionId,
				trackerId,
				title,
				title,
				null,
				null,
				"Refresh in Progress",
				null,
				2,
				0,
				0,
				false,
				0,
				false,
				assignedUsername,
				null,
				planningFolder,
				null,
				flexFields,
				null);

			SoapFieldValues flex = asd.getFlexFields();
			int m = 0;
			Object[] values = flex.getValues();

			for (String s : flex.getNames())
			{
				if (s.equals("Refresh ID"))
				{
					values[m] = asd.getId().replace("artf", "RF");
					break;
				}

				m += 1;
			}

			flex.setValues(values);
			asd.setFlexFields(flex);
			setArtifactData(asd, "Set Refresh ID");
			m_iRetry = NUM_RETRIES;
			return asd;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return createRefreshArtifact(
					trackerId,
					assignedUsername,
					application,
					sourceEnvironment,
					targetEnvironment,
					refreshDate,
					planningFolder);
			}
			else
			{
				errorMsg("Unable to create refresh artifact for '" + title + "': " + sMsg, e);
			}
		}

		return null;
	}

	public void createArtifactDependency(String parentArtifactId, String childArtifactId)
	{
		try
		{
			m_trackerSoap.createArtifactDependency(m_sSessionId, parentArtifactId, childArtifactId, null);
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				createArtifactDependency(parentArtifactId, childArtifactId);
			}
			else
			{
				errorMsg("Unable to create dependency betwwen " +  parentArtifactId + " and " + childArtifactId + ": " + sMsg, e);
			}
		}
	}

	public DocumentFolderSoapRow[] getDocumentFolderList(String parentId)
	{
		try
		{
			DocumentFolderSoapList dfsl = m_docSoap.getDocumentFolderList(m_sSessionId, parentId, false);
			DocumentFolderSoapRow[] dfRows = dfsl.getDataRows();
			m_iRetry = NUM_RETRIES;
			return dfRows;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getDocumentFolderList(parentId);
			}
			else
			{
				errorMsg("Unable to retrieve document folders for parent " + parentId + ": " + sMsg, e);
			}
		}

		return null;
	}

	public String createDocumentFolder(String parentId, String title, String description)
	{
		try
		{
			DocumentFolderSoapDO dfsd = m_docSoap.createDocumentFolder(m_sSessionId, parentId, title, description);
			System.out.println("Folder created successfully");
			return dfsd.getId();
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return createDocumentFolder(parentId, title, description);
			}
			else
			{
				errorMsg("Unable to create document folder " +  title + " below " + parentId + ": " + sMsg, e);
			}
		}

		return null;
	}

	public DocumentSoapRow getDocument(String folderId, String title)
	{
		DocumentSoapRow dsr = null;

		try
		{
			DocumentSoapList dsl = m_docSoap.getDocumentList(m_sSessionId, folderId, null);

			for (int i = 0; i < dsl.getDataRows().length; ++i)
			{
				if (dsl.getDataRows()[i].getTitle().equals(title))
				{
					dsr = dsl.getDataRows()[i];
					break;
				}
			}

			m_iRetry = NUM_RETRIES;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				return getDocument(folderId, title);
			}
		}

		return dsr;
	}

	public void createDocumentWithUrl(String parentId, String title, String description, String url, String artifactId)
	{
		try
		{
			m_docSoap.createDocumentWithUrl(m_sSessionId, parentId, title, description, null, "Final", false, url, artifactId, null);
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				createDocumentWithUrl(parentId, title, description, url, artifactId);
			}
			else
			{
				errorMsg("Unable to create document " + title + " with url " +  url + " below " + parentId + ": " + sMsg, e);
			}
		}
	}

	public void deleteDocument(String documentId)
	{
		try
		{
			m_docSoap.deleteDocument(m_sSessionId, documentId);
			m_iRetry = NUM_RETRIES;
		}
		catch (java.rmi.RemoteException e)
		{
			String sMsg = e.getMessage();

			if (retryAfterException(sMsg))
			{
				deleteDocument(documentId);
			}
			else
			{
				errorMsg("Unable to delete document " + documentId + ": " + sMsg, e);
			}
		}
	}
}