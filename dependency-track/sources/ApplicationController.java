/*
 * This file is part of Dependency-Track.
 *
 * Dependency-Track is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Dependency-Track is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Dependency-Track. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) Axway. All Rights Reserved.
 */
package org.owasp.dependencytrack.controller;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.owasp.dependencycheck.reporting.ReportGenerator;
import org.owasp.dependencytrack.model.Application;
import org.owasp.dependencytrack.model.ApplicationVersion;
import org.owasp.dependencytrack.service.ApplicationService;
import org.owasp.dependencytrack.service.ApplicationVersionService;
import org.owasp.dependencytrack.service.LibraryVersionService;
import org.owasp.dependencytrack.service.ReportService;
import org.owasp.dependencytrack.service.VulnerabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

// Plugin imports
import java.util.List;
import org.owasp.dependencytrack.model.Library;
import org.owasp.dependencytrack.model.Vulnerability;
import org.owasp.dependencytrack.model.LibraryVersion;
import org.owasp.dependencytrack.dao.ApplicationDao;
import org.owasp.dependencytrack.service.LibraryVersionService;
import org.owasp.dependencytrack.service.VulnerabilityService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import java.io.File;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.util.ArrayList;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

// Plugin imports

/**
 * Controller logic for all Application-related requests.
 * 
 * @author Steve Springett (steve.springett@owasp.org)
 */
@Controller
public class ApplicationController extends AbstractController {

	/**
	 * Setup logger
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ApplicationController.class);

	/* PLUGGIN */
	@Autowired
	private ApplicationDao applicationDao;

	/**
	 * The Dependency-Track ApplicationService.
	 */
	@Autowired
	private ApplicationService applicationService;

	/**
	 * The Dependency-Track ApplicationVersionService.
	 */
	@Autowired
	private ApplicationVersionService applicationVersionService;

	/**
	 * The Dependency-Track VulnerabilityService.
	 */
	@Autowired
	private VulnerabilityService vulnerabilityService;

	/**
	 * The Dependency-Track LibraryVersionService.
	 */
	@Autowired
	private LibraryVersionService libraryVersionService;

	/**
	 * The Dependency-Track ReportService.
	 */
	@Autowired
	private ReportService reportService;

	/**
	 * Initialization method gets called after controller is constructed.
	 */
	@PostConstruct
	public void init() {
		LOGGER.info("OWASP Dependency-Track Initialized");
	}

	/**
	 * Lists all applications.
	 * 
	 * @param map
	 *            A map of parameters
	 * @param request
	 *            a HttpServletRequest object
	 * @return a String
	 */
	@RequiresPermissions("applications")
	@RequestMapping(value = "/applications", method = RequestMethod.GET)
	public String application(Map<String, Object> map,
			HttpServletRequest request) {
		map.put("check", false);
		map.put("applicationList", applicationService.listApplications());
		return "applicationsPage";
	}

	/**
	 * Lists vulnerability summary information for the specified application.
	 * 
	 * @param map
	 *            A map of parameters
	 * @param id
	 *            The ID of the Application to retrieve vulnerability info for
	 * @return a String
	 */
	@RequiresPermissions("applications")
	@RequestMapping(value = "/vulnerabilitySummary/{id}", method = RequestMethod.GET)
	public String vulnerabiltySummary(Map<String, Object> map,
			@PathVariable("id") int id) {
		map.put("vulnerabilityInfo",
				vulnerabilityService.getVulnerabilitySummary(id));
		return "vulnerabilitySummary";
	}

	/**
	 * Dynamically generates a native Dependency-Check XML report.
	 * 
	 * @param map
	 *            A map of parameters
	 * @param id
	 *            The ID of the Applicaiton to create a report for
	 * @return A String representation of the XML report
	 */
	@RequestMapping(value = "/dependencyCheckReport/{id}.xml", method = RequestMethod.GET, produces = "application/xml")
	@ResponseBody
	public String dependencyCheckXmlReport(Map<String, Object> map,
			@PathVariable("id") int id) {
		return reportService.generateDependencyCheckReport(id,
				ReportGenerator.Format.XML);
	}

	/**
	 * Dynamically generates a native Dependency-Check HTML report.
	 * 
	 * @param map
	 *            A map of parameters
	 * @param id
	 *            The ID of the Applicaiton to create a report for
	 * @return A String representation of the HTML report
	 */
	@RequestMapping(value = "/dependencyCheckReport/{id}.html", method = RequestMethod.GET, produces = "text/html")
	@ResponseBody
	public String dependencyCheckHtmlReport(Map<String, Object> map,
			@PathVariable("id") int id) {
		return reportService.generateDependencyCheckReport(id,
				ReportGenerator.Format.HTML);
	}

	/**
	 * Search action.
	 * 
	 * @param map
	 *            a map of parameters
	 * @param libid
	 *            the ID of the Library to search on
	 * @param libverid
	 *            The ID of the LibraryVersion to search on
	 * @return a String
	 */
	@RequiresPermissions("searchApplication")
	@RequestMapping(value = "/searchApplication", method = RequestMethod.POST)
	public String searchApplication(Map<String, Object> map,
			@RequestParam("serapplib") int libid,
			@RequestParam("serapplibver") int libverid) {

		if (libverid != -1) {
			map.put("applicationList",
					applicationService.searchApplications(libverid));
			map.put("versionlist",
					applicationService.searchApplicationsVersion(libverid));
			map.put("check", true);
		} else {

			map.put("applicationList",
					applicationService.searchAllApplications(libid));
			map.put("versionlist",
					applicationService.searchAllApplicationsVersions(libid));
			map.put("check", true);
		}
		return "applicationsPage";
	}

	/**
	 * Search action.
	 * 
	 * @param map
	 *            a map of parameters
	 * @param vendorId
	 *            The ID of the Vendor to search on
	 * @return a String
	 */
	@RequiresPermissions("coarseSearchApplication")
	@RequestMapping(value = "/coarseSearchApplication", method = RequestMethod.POST)
	public String coarseSearchApplication(Map<String, Object> map,
			@RequestParam("coarseSearchVendor") int vendorId) {

		map.put("applicationList",
				applicationService.coarseSearchApplications(vendorId));
		map.put("versionlist",
				applicationService.coarseSearchApplicationVersions(vendorId));
		map.put("check", true);
		return "applicationsPage";
	}

	/**
	 * Search action.
	 * 
	 * @param map
	 *            a map of parameters
	 * @param searchTerm
	 *            is the search term
	 * @return a String
	 */
	@RequiresPermissions("keywordSearchLibraries")
	@RequestMapping(value = "/keywordSearchLibraries", method = RequestMethod.POST)
	public String keywordSearchLibraries(Map<String, Object> map,
			@RequestParam("keywordSearchVendor") String searchTerm) {
		map.put("libList",
				libraryVersionService.keywordSearchLibraries(searchTerm));
		return "librariesPage";
	}

	/**
	 * Add Application action. Adds an application and associated version number
	 * 
	 * @param application
	 *            The Application to add
	 * @param version
	 *            a String of the version number to add
	 * @return a String
	 */
	@RequiresPermissions("addApplication")
	@RequestMapping(value = "/addApplication", method = RequestMethod.POST)
	public String addApplication(
			@ModelAttribute("application") Application application,
			@RequestParam("version") String version) {
		applicationService.addApplication(application, version);
		return "redirect:/applications";
	}

	/**
	 * Updates an applications' name.
	 * 
	 * @param id
	 *            The ID of the application to update
	 * @param name
	 *            The updated name of the application
	 * @return a String
	 */
	@RequiresPermissions("updateApplication")
	@RequestMapping(value = "/updateApplication", method = RequestMethod.POST)
	public String updatingProduct(@RequestParam("id") int id,
			@RequestParam("name") String name) {
		applicationService.updateApplication(id, name);
		return "redirect:/applications";
	}

	/**
	 * Updates an applications' version.
	 * 
	 * @param id
	 *            The ID of the ApplicationVersion
	 * @param appversion
	 *            The version label
	 * @return a String
	 */
	@RequiresPermissions("updateApplicationVersion")
	@RequestMapping(value = "/updateApplicationVersion", method = RequestMethod.POST)
	public String updatingApplicationVersion(
			@RequestParam("appversionid") int id,
			@RequestParam("editappver") String appversion) {
		applicationVersionService.updateApplicationVersion(id, appversion);
		return "redirect:/applications";
	}

	/**
	 * Deletes the application with the specified id.
	 * 
	 * @param id
	 *            The ID of the Application to delete
	 * @return a String
	 */
	@RequiresPermissions("deleteApplication")
	@RequestMapping(value = "/deleteApplication/{id}", method = RequestMethod.GET)
	public String removeApplication(@PathVariable("id") int id) {
		applicationService.deleteApplication(id);
		return "redirect:/applications";
	}

	/**
	 * Deletes the application Version with the specified id.
	 * 
	 * @param id
	 *            The ID of the ApplicationVersion to delete
	 * @return a String
	 */
	@RequiresPermissions("deleteApplicationVersion")
	@RequestMapping(value = "/deleteApplicationVersion/{id}", method = RequestMethod.GET)
	public String deleteApplicationVersion(@PathVariable("id") int id) {

		applicationVersionService.deleteApplicationVersion(id);
		return "redirect:/applications";
	}

	/**
	 * Adds a version to an application.
	 * 
	 * @param id
	 *            The ID of the Application
	 * @param version
	 *            The version label
	 * @return a String
	 */
	@RequiresPermissions("addApplicationVersion")
	@RequestMapping(value = "/addApplicationVersion", method = RequestMethod.POST)
	public String addApplicationVersion(@RequestParam("id") int id,
			@RequestParam("version") String version) {
		applicationVersionService.addApplicationVersion(id, version);
		return "redirect:/applications";
	}

	/**
	 * Lists the data in the specified application version.
	 * 
	 * @param modelMap
	 *            a Spring ModelMap
	 * @param map
	 *            a map of parameters
	 * @param id
	 *            the ID of the Application to list versions for
	 * @return a String
	 */
	@RequiresPermissions("applicationVersion")
	@RequestMapping(value = "/applicationVersion/{id}", method = RequestMethod.GET)
	public String listApplicationVersion(ModelMap modelMap,
			Map<String, Object> map, @PathVariable("id") int id) {
		final ApplicationVersion version = applicationVersionService
				.getApplicationVersion(id);
		modelMap.addAttribute("id", id);
		map.put("applicationVersion", version);
		map.put("dependencies", libraryVersionService.getDependencies(version));
		map.put("libraryVendors", libraryVersionService.getLibraryHierarchy());
		return "applicationVersionPage";
	}

	/**
	 * Adds a ApplicationDependency between the specified ApplicationVersion and
	 * LibraryVersion.
	 * 
	 * @param appversionid
	 *            The ID of the ApplicationVersion
	 * @param versionid
	 *            The ID of the LibraryVersion
	 * @return a String
	 */
	@RequiresPermissions("addDependency")
	@RequestMapping(value = "/addDependency", method = RequestMethod.POST)
	public String addDependency(@RequestParam("appversionid") int appversionid,
			@RequestParam("versionid") int versionid) {
		libraryVersionService.addDependency(appversionid, versionid);
		return "redirect:/applicationVersion/" + appversionid;
	}

	/**
	 * Deletes the dependency with the specified ApplicationVersion ID and
	 * LibraryVersion ID.
	 * 
	 * @param appversionid
	 *            The ID of the ApplicationVersion
	 * @param versionid
	 *            The ID of the LibraryVersion
	 * @return a String
	 */
	@RequiresPermissions("deleteDependency")
	@RequestMapping(value = "/deleteDependency", method = RequestMethod.GET)
	public String deleteDependency(
			@RequestParam("appversionid") int appversionid,
			@RequestParam("versionid") int versionid) {
		libraryVersionService.deleteDependency(appversionid, versionid);
		return "redirect:/applicationVersion/" + appversionid;
	}

	/**
	 * Clone the Application including all ApplicationVersions.
	 * 
	 * @param applicationid
	 *            The ID of the Application to clone
	 * @param applicationname
	 *            The name of the cloned Application
	 * @return a String
	 */
	@RequiresPermissions("cloneApplication")
	@RequestMapping(value = "/cloneApplication", method = RequestMethod.POST)
	public String cloneApplication(
			@RequestParam("applicationid") int applicationid,
			@RequestParam("cloneAppName") String applicationname) {
		applicationVersionService.cloneApplication(applicationid,
				applicationname);
		return "redirect:/applications";
	}

	/**
	 * Clone the ApplicationVersion.
	 * 
	 * @param applicationid
	 *            The ID of the Application to clone
	 * @param newversion
	 *            The version of the cloned ApplicationVersion
	 * @param applicationversion
	 *            The ApplicationVersion to clone
	 * @return a String
	 */
	@RequiresPermissions("cloneApplicationVersion")
	@RequestMapping(value = "/cloneApplicationVersion", method = RequestMethod.POST)
	public String cloneApplicationVersion(
			@RequestParam("applicationid") int applicationid,
			@RequestParam("cloneVersionNumber") String newversion,
			@RequestParam("applicationversion") String applicationversion) {
		applicationVersionService.cloneApplicationVersion(applicationid,
				newversion, applicationversion);
		return "redirect:/applications";
	}

	/**
	 * Lists the vulnerability data in the specified application version.
	 * 
	 * @param modelMap
	 *            a Spring ModelMap
	 * @param map
	 *            a map of parameters
	 * @param id
	 *            the ID of the Application to list versions for
	 * @return a String
	 */
	@RequiresPermissions("vulnerabilities")
	@RequestMapping(value = "/vulnerabilities/{id}", method = RequestMethod.GET)
	public String listVulnerabilityData(ModelMap modelMap,
			Map<String, Object> map, @PathVariable("id") int id) {
		final ApplicationVersion version = applicationVersionService
				.getApplicationVersion(id);
		modelMap.addAttribute("id", id);
		map.put("applicationVersion", version);
		map.put("dependencies", libraryVersionService.getDependencies(version));
		map.put("libraryVendors", libraryVersionService.getLibraryHierarchy());
		map.put("vulnerableComponents",
				vulnerabilityService.getVulnerableComponents(version));
		return "vulnerabilitiesPage";
	}

	/**
	 * Performs an immediate scan against all library versions.
	 * 
	 * @return a String
	 */
	@RequiresRoles("admin")
	@RequestMapping(value = "/about/scan", method = RequestMethod.GET)
	public String scanNow() {
		vulnerabilityService.initiateFullDependencyCheckScan();
		return "redirect:/about";
	}

	/**
	 * Upload a License.
	 * 
	 * @param licenseid
	 *            the ID of the License to download
	 * @param file
	 *            the license file to upload
	 * @param editlicensename
	 *            an updated license name
	 * @return a String
	 */
	@RequiresPermissions("uploadlicense")
	@RequestMapping(value = "/uploadlicense", method = RequestMethod.POST)
	public String uploadLicense(
			@RequestParam("uploadlicenseid") Integer licenseid,
			@RequestParam("uploadlicensefile") MultipartFile file,
			@RequestParam("editlicensename") String editlicensename) {
		libraryVersionService.uploadLicense(licenseid, file, editlicensename);
		return "redirect:/libraries";
	}

	/**
	 * Limits what fields can be automatically bound.
	 * 
	 * @param binder
	 *            a WebDataBinder object
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		if (binder.getTarget() instanceof Application) {
			binder.setAllowedFields("name");
		}
	}

	/**
	 * The about page.
	 * 
	 * @return a String
	 */
	@RequiresPermissions("about")
	@RequestMapping(value = "/about", method = RequestMethod.GET)
	public String about() {
		return "aboutPage";
	}

	/**
	 * Plugin.
	 * 
	 * @return a String
	 */
	@RequiresPermissions("about")
	@RequestMapping(value = "/plugin", method = RequestMethod.GET)
	public String plugin() {
		PLUGIN_MAIN();
		return "aboutPage";
	}

	/**
	 * Hierarchy.
	 * 
	 * @return a String
	 */
	@RequiresPermissions("about")
	@RequestMapping(value = "/hierarchy", method = RequestMethod.GET)
	public String hierarchy() {
		HIERARCHY_MAIN();
		return "aboutPage";
	}

	/* ---------------------------------------------------------------------- */
	/* ---------------------------------------------------------------------- */
	/* ----------------------- THE PLUGIN'S FUNCTIONS ---------------------- */
	/* ---------------------------------------------------------------------- */
	/* ---------------------------------------------------------------------- */

	public void PLUGIN_MAIN() {

		try {
			/* Opening the JSON file and getting a JSONObject. */

			// The "program_info.json" file has to be outside of the war file.
			JSONObject obj = _fileToJSONObject("/var/opt/dependency-track-pluggin/program_info.json");

			/* Extracting the information from the JSONObject. */

			@SuppressWarnings("unchecked")
			Set<String> programs = (Set<String>) obj.keySet();

			for (String program : programs) {
				JSONArray array = (JSONArray) obj.get(program);
				JSONArray componentsProgram = (JSONArray) array.get(0);
				String versionProgram = (String) array.get(1);
				// We have now : program, components, version of a program

				/* Saving the applications in the database */
				String idApplicationVersion = _addApplication(program,
						versionProgram);

				int len = componentsProgram.size();
				for (int i = 0; i < len; i = i + 1) {
					@SuppressWarnings("unchecked")
					ArrayList<String> component = (ArrayList<String>) componentsProgram
							.get(i);
					String vendorComponent = component.get(0);
					String productComponent = component.get(1);
					String versionComponent = component.get(2);
					String languageComponent = component.get(3);
					String licenseComponent = component.get(4);
					// We have now : vendor, product, version, language, license
					// of a component

					/* Saving the library in the database. */
					String idLibraryVersion = _addLibrary(productComponent,
							versionComponent, vendorComponent,
							licenseComponent, languageComponent);

					/* Adding the dependencies. */
					_addDependencies(idApplicationVersion, idLibraryVersion);

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public JSONObject _fileToJSONObject(String pathfile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(pathfile));
			String s = br.readLine();
			Object obj1 = JSONValue.parse(s);
			br.close();
			JSONObject obj2 = (JSONObject) obj1;
			return obj2;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public String _addApplication(String program, String versionProgram) {

		Application app = new Application();
		app.setName(program);
		applicationDao.addApplication(app, versionProgram);

		return _getIdApplicationVersion(program, versionProgram);

	}

	public String _addLibrary(String productComponent, String versionComponent,
			String vendorComponent, String licenseComponent,
			String languageComponent) {

		MultipartFile multipartFile = _createMultipartFile();

		libraryVersionService.addLibraries(productComponent, versionComponent,
				vendorComponent, licenseComponent, multipartFile,
				languageComponent);

		return _getIdLibraryVersion(productComponent, versionComponent);
	}

	public MultipartFile _createMultipartFile() {

		try {

			File file = new File("");
			DiskFileItem fileItem = new DiskFileItem("file", "text/plain",
					false, file.getName(), (int) file.length(),
					file.getParentFile());
			fileItem.getOutputStream();
			MultipartFile multipartFile = new CommonsMultipartFile(fileItem);
			return multipartFile;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public String _getIdApplicationVersion(String program, String versionProgram) {

		for (Application app : applicationService.listApplications()) {

			String name = app.getName().toString();

			if (name.equals(program)) {

				for (ApplicationVersion appvers : app.getVersions()) {

					String version = appvers.getVersion().toString();

					if (version.equals(versionProgram)) {
						return appvers.getId().toString();
					}
				}
			}
		}

		return "";
	}

	public String _getIdLibraryVersion(String productComponent,
			String versionComponent) {

		List<LibraryVersion> libraryVersions = libraryVersionService
				.allLibrary();

		for (LibraryVersion libraryVersion : libraryVersions) {

			String version = libraryVersion.getLibraryversion();

			if (versionComponent.equals(version)) {

				Library library = libraryVersion.getLibrary();
				String name = library.getLibraryname();

				if (productComponent.equals(name)) {
					return libraryVersion.getId().toString();
				}
			}
		}

		return "";
	}

	public void _addDependencies(String idApplicationVersion,
			String idLibraryVersion) {
		libraryVersionService.addDependency(
				Integer.parseInt(idApplicationVersion),
				Integer.parseInt(idLibraryVersion));
	}

public static void _writeInFile(String str, String filePath) {

		Writer writer = null;

		try {

			writer = new FileWriter(filePath, true);
			writer.write(str);
			writer.write('\n');

		} catch (IOException e) {

			System.err.println("Error writing the file : ");
			e.printStackTrace();

		} finally {

			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {

					System.err.println("Error closing the file : ");
					e.printStackTrace();
				}
			}

		}
	}

	public static void _initializeFile(String filePath) {

		Writer writer = null;

		try {
			writer = new FileWriter(filePath);
		} catch (IOException e) {
			System.err.println("Error writing the file : ");
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					System.err.println("Error closing the file : ");
					e.printStackTrace();
				}
			}
		}
	}

	public static void _LOGTEST(String str) {
		String filePath = "/var/opt/dependency-track-pluggin/logtest.txt";
		_writeInFile(str, filePath);
	}

	public void HIERARCHY_MAIN() {

		String output = "/var/opt/dependency-track-pluggin/gephi.csv";
		_initializeFile(output);
		_writeInFile("Source,Target", output);

		String prefixApp = "app:";
		String prefixDep = "dep:";

		for (Application app : applicationService.listApplications()) {
			// app : Application

			String name = app.getName().toString();
			// name : Application name

			for (ApplicationVersion appvers : app.getVersions()) {
				// appvers : ApplicationVersion

				String version = appvers.getVersion().toString();
				// version : Application version

				for (LibraryVersion libvers : libraryVersionService
						.getDependencies(appvers)) {
					// libvers : LibraryVersion

					String libversion = libvers.getLibraryversion().toString();
					String libname = libvers.getLibrary().getLibraryname()
							.toString();
					// libversion: Library version ; libname : Library name

					_writeInFile(prefixApp + name + ":" + version + ","
							+ prefixDep + libname + ":" + libversion, output);

					for (Vulnerability vulnerability : vulnerabilityService
							.getVulnsForLibraryVersion(libvers)) {

						String vulnname = vulnerability.getName().toString();

						/* Writting in the file */
						_writeInFile(prefixDep + libname + ":" + libversion
								+ "," + vulnname, output);
					}
				}
			}
		}
	}



}
