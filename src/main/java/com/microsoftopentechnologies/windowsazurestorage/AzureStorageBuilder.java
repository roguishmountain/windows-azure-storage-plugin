/*
 Copyright 2014 Microsoft Open Technologies, Inc.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.microsoftopentechnologies.windowsazurestorage;

import java.io.File;
import java.util.Locale;

import jenkins.tasks.SimpleBuildStep;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.microsoftopentechnologies.windowsazurestorage.beans.StorageAccountInfo;
import com.microsoftopentechnologies.windowsazurestorage.helper.Utils;
import hudson.DescriptorExtensionList;
import hudson.EnvVars;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.plugins.copyartifact.*;
import hudson.security.AccessControlled;
import hudson.tasks.BuildStepDescriptor;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import static hudson.tasks.BuildStepDescriptor.filter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.kohsuke.stapler.AncestorInPath;


public class AzureStorageBuilder extends Builder implements SimpleBuildStep {

	private String storageAccName;
	private String containerName;
	private String includeFilesPattern;
	private String excludeFilesPattern;
	private String downloadDirLoc;
	private boolean flattenDirectories;
	private boolean includeArchiveZips;
        private BuildSelector buildSelector; 
        private String projectName;

	@DataBoundConstructor
	public AzureStorageBuilder(String storageAccName, String containerName, BuildSelector buildSelector,
			String includeFilesPattern, String excludeFilesPattern, String downloadDirLoc, boolean flattenDirectories,
			boolean includeArchiveZips, String projectName) {
		this.storageAccName = storageAccName;
		this.containerName = containerName;
		this.includeFilesPattern = includeFilesPattern;
		this.excludeFilesPattern = excludeFilesPattern;
		this.downloadDirLoc = downloadDirLoc;
		this.flattenDirectories = flattenDirectories;
		this.includeArchiveZips = includeArchiveZips;
                this.projectName = projectName;
                if (buildSelector == null) {
                    buildSelector = new StatusBuildSelector(true);
                }
                this.buildSelector = buildSelector;
	}

        public BuildSelector getBuildSelector() {
            return buildSelector;
        }
        
	public String getStorageAccName() {
		return storageAccName;
	}

	public void setStorageAccName(String storageAccName) {
		this.storageAccName = storageAccName;
	}

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}
        
        public String getProjectName() {
            return projectName;
        }
        
        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

	public String getIncludeFilesPattern() {
		return includeFilesPattern;
	}

	public void setIncludeFilesPattern(String excludeFilesPattern) {
		this.includeFilesPattern = excludeFilesPattern;
	}
	
	public String getExcludeFilesPattern() {
		return excludeFilesPattern;
	}

	public void setExcludeFilesPattern(String excludeFilesPattern) {
		this.excludeFilesPattern = excludeFilesPattern;
	}

	public String getDownloadDirLoc() {
		return downloadDirLoc;
	}

	public void setDownloadDirLoc(String downloadDirLoc) {
		this.downloadDirLoc = downloadDirLoc;
	}
	
	public boolean isIncludeArchiveZips() {
		return includeArchiveZips;
	}
	
	public void setIncludeArchiveZips(final boolean includeArchiveZips) {
		this.includeArchiveZips = includeArchiveZips;
	}
	
	public boolean isFlattenDirectories() {
		return flattenDirectories;
	}
	
	public void setFlattenDirectories(final boolean flattenDirectories){
		this.flattenDirectories = flattenDirectories;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	public void perform(Run<?,?> run, FilePath filePath, Launcher launch, TaskListener listener) {
		StorageAccountInfo strAcc = null;
                
		try {
                        // TODO:
                        final EnvVars envVars = run.getEnvironment(listener);
			// Get storage account
			strAcc = getDescriptor().getStorageAccount(storageAccName);
                        
                        Job<?,?> job = Jenkins.getInstance().getItemByFullName(projectName, Job.class);
                        BuildFilter filter = new BuildFilter();
                        
                        listener.getLogger().println(getBuildSelector());
                        
                        Run source = getBuildSelector().getBuild(job, envVars, filter, run);
                        // listener.getLogger().println("get build info");
                        final AzureBlobAction action = source.getAction(AzureBlobAction.class);
                        List<AzureBlob> blob = action.getIndividualBlobs();
                        for(AzureBlob item: blob){
                            listener.getLogger().println("item: " + item.getBlobURL());
                            URL blobURL = new URL(item.getBlobURL());
                            listener.getLogger().println(blobURL.getFile());
                        }
                        
//                        if(getBuildSelector() instanceof PermalinkBuildSelector) {
//                            listener.getLogger().println(filter.isSelectable(run, envVars));
//                            listener.getLogger().println("source: " + source);
//                        }
//                        else {
//                            listener.getLogger().println("" + source.number);
//                            listener.getLogger().println("" + source.getParent().getFullDisplayName());
//                            EnvVars parentEnvVars = source.getEnvironment(null);
//                            listener.getLogger().println("manage artifacts: " + parentEnvVars.get("manageArtifacts"));
//                        }
                        
			// Resolve container name
//			String expContainerName = Util.replaceMacro(containerName, envVars);
//			if (expContainerName != null) {
//				expContainerName = expContainerName.trim().toLowerCase(
//						Locale.ENGLISH);
//			}

			// Resolve include patterns
			String expIncludePattern = Util.replaceMacro(includeFilesPattern, envVars);
			
			// Resolve exclude patterns
			String expExcludePattern = Util.replaceMacro(excludeFilesPattern, envVars);
			
			// If the include is empty, make **/*
			if (Utils.isNullOrEmpty(expIncludePattern)) {
				expIncludePattern = "**/*";
			}
			
			// Exclude archive.zip by default.
			if (!includeArchiveZips) {
				if (expExcludePattern != null) {
					expExcludePattern += ",archive.zip";
				}
				else {
					expExcludePattern = "archive.zip";
				}
			}

			// Resolve download location
			String downloadDir = Util.replaceMacro(downloadDirLoc, envVars);

//			// Validate input data
//			if (!validateData(build, taskListener, strAcc, expContainerName)) {
//				return true; // returning true so that build can continue.
//			}

			int filesDownloaded = WAStorageClient.download(run, listener,
					strAcc, blob, expIncludePattern, expExcludePattern, 
					downloadDir, flattenDirectories);

			if (filesDownloaded == 0) { // Mark build unstable if no files are
										// downloaded
				listener.getLogger().println(
						Messages.AzureStorageBuilder_nofiles_downloaded());
				run.setResult(Result.UNSTABLE);
			} else {
				listener.getLogger()
						.println(
								Messages.AzureStorageBuilder_files_downloaded_count(filesDownloaded));
			}
		} catch (Exception e) {
			e.printStackTrace(listener.error(Messages
					.AzureStorageBuilder_download_err(strAcc
							.getStorageAccName())));
			run.setResult(Result.UNSTABLE);
		}
	}

	private boolean validateData(AbstractBuild build, BuildListener listener,
			StorageAccountInfo strAcc, String expContainerName) {

		// No need to download artifacts if build failed
		if (build.getResult() == Result.FAILURE) {
			listener.getLogger().println(
					Messages.AzureStorageBuilder_build_failed_err());
			return false;
		}

		if (strAcc == null) {
			listener.getLogger().println(
					Messages.WAStoragePublisher_storage_account_err());
			build.setResult(Result.UNSTABLE);
			return false;
		}

		// Validate container name
		if (!Utils.validateContainerName(expContainerName)) {
			listener.getLogger().println(
					Messages.WAStoragePublisher_container_name_err());
			build.setResult(Result.UNSTABLE);
			return false;
		}

		// Check if storage account credentials are valid
		try {
			WAStorageClient.validateStorageAccount(strAcc.getStorageAccName(),
					strAcc.getStorageAccountKey(), strAcc.getBlobEndPointURL());
		} catch (Exception e) {
			listener.getLogger().println(Messages.Client_SA_val_fail());
			listener.getLogger().println(strAcc.getStorageAccName());
			listener.getLogger().println(strAcc.getBlobEndPointURL());
			build.setResult(Result.UNSTABLE);
			return false;
		}
		return true;
	}

	public AzureStorageBuilderDesc getDescriptor() {
		// see Descriptor javadoc for more about what a descriptor is.
		return (AzureStorageBuilderDesc) super.getDescriptor();
	}

	@Extension
	public static final class AzureStorageBuilderDesc extends
			Descriptor<Builder> {

		public boolean isApplicable(
				@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
			return true;
		}

		public AzureStorageBuilderDesc() {
			super();
			load();
		}

		// Can be used in future for dynamic display in UI
		/*
		 * private List<String> getContainersList(String StorageAccountName) {
		 * try { return WAStorageClient.getContainersList(
		 * getStorageAccount(StorageAccountName), false); } catch (Exception e)
		 * { e.printStackTrace(); return null; } }
		 * 
		 * private List<String> getBlobsList(String StorageAccountName, String
		 * containerName) { try { return WAStorageClient.getContainerBlobList(
		 * getStorageAccount(StorageAccountName), containerName); } catch
		 * (Exception e) { e.printStackTrace(); return null; } }
		 */

		public ListBoxModel doFillStorageAccNameItems() {
			ListBoxModel m = new ListBoxModel();
			StorageAccountInfo[] StorageAccounts = getStorageAccounts();

			if (StorageAccounts != null) {
				for (StorageAccountInfo storageAccount : StorageAccounts) {
					m.add(storageAccount.getStorageAccName());
				}
			}
			return m;
		}

		/*
		 * public ComboBoxModel doFillContainerNameItems(
		 * 
		 * @QueryParameter String storageAccName) { ComboBoxModel m = new
		 * ComboBoxModel();
		 * 
		 * List<String> containerList = getContainersList(storageAccName); if
		 * (containerList != null) { m.addAll(containerList); } return m; }
		 * 
		 * public ComboBoxModel doFillBlobNameItems(
		 * 
		 * @QueryParameter String storageAccName,
		 * 
		 * @QueryParameter String containerName) { ComboBoxModel m = new
		 * ComboBoxModel();
		 * 
		 * List<String> blobList = getBlobsList(storageAccName, containerName);
		 * if (blobList != null) { m.addAll(blobList); } return m; }
		 * 
		 * public AutoCompletionCandidates
		 * doAutoCompleteBlobName(@QueryParameter String storageAccName,
		 * 
		 * @QueryParameter String containerName) { List<String> blobList =
		 * getBlobsList(storageAccName, containerName); AutoCompletionCandidates
		 * autoCand = null;
		 * 
		 * if (blobList != null ) { autoCand = new AutoCompletionCandidates();
		 * autoCand.add(blobList.toArray(new String[blobList.size()])); } return
		 * autoCand; }
		 */

		/* public FormValidation doCheckIsDirectory(@QueryParameter String val) {
			// If null or if file does not exists don't display any validation
			// error.
			File downloadDir = new File(val);
			if (Utils.isNullOrEmpty(val) || !downloadDir.exists()) {
				return FormValidation.ok();
			} else if (downloadDir.exists() && !downloadDir.isDirectory()) {
				return FormValidation.error(Messages
						.AzureStorageBuilder_downloadDir_invalid());
			} else {
				return FormValidation.ok();
			}
		} */

		public boolean configure(StaplerRequest req, JSONObject formData)
				throws FormException {
			save();
			return super.configure(req, formData);
		}

		public String getDisplayName() {
			return Messages.AzureStorageBuilder_displayName();
		}

		public StorageAccountInfo[] getStorageAccounts() {
			WAStoragePublisher.WAStorageDescriptor publisherDescriptor = Jenkins
					.getInstance().getDescriptorByType(
							WAStoragePublisher.WAStorageDescriptor.class);

			StorageAccountInfo[] sa = publisherDescriptor.getStorageAccounts();

			return sa;
		}

		/**
		 * Returns storage account object
		 * 
		 * @return StorageAccount
		 */
		public StorageAccountInfo getStorageAccount(String storageAccountName) {
			if ((storageAccountName == null)
					|| (storageAccountName.trim().length() == 0)) {
				return null;
			}

			StorageAccountInfo storageAcc = null;
			StorageAccountInfo[] StorageAccounts = getStorageAccounts();

			if (StorageAccounts != null) {
				for (StorageAccountInfo sa : StorageAccounts) {
					if (sa.getStorageAccName().equals(storageAccountName)) {
						storageAcc = sa;

						storageAcc.setBlobEndPointURL(Utils.getBlobEP(
								storageAcc.getBlobEndPointURL()));
						break;
					}
				}
			}
			return storageAcc;
		}
	   }

    // TODO
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> clazz) {
            return true;
        }

        @Override
        public String getDisplayName() {
            // where does this go? 
            return "Artifact...";
        }

        public DescriptorExtensionList<BuildSelector, Descriptor<BuildSelector>> getBuildSelectors() {
            final DescriptorExtensionList<BuildSelector, Descriptor<BuildSelector>> list = DescriptorExtensionList.createDescriptorList(Jenkins.getInstance(), BuildSelector.class);
            return list;
        }
    }

}
