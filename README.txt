Enterprise Content Management(ECM) > Workflow Management System(WF)
Version 1.0.6

You may find it helpful to see the details at wiki place of ECM
http://wiki.exoplatform.org/xwiki/bin/view/ECM/

TABLE OF CONTENTS
---------------------------------------------------
1. What is eXo Workflow
2. How to set up eXo Workflow
3. Release notes
4. Migration guide


1. WHAT IS EXO WORKFLOW
---------------------------------------------------
		Workflow: is the way of looking at and controlling the processes presented in an organization such as service provision or 
information processing, etc. It is an effective tool to use in times of crisis to make certain that the processes are efficient and 
effective with the purpose of better and more cost efficient organization.
	
2. HOW TO SET UP EXO WORKFLOW
---------------------------------------------------
eXo Workflow requires the Java 2 Standard Edition Runtime Environment (JRE) or Java Development Kit version 5.x

2.1. Install Java SE 1.5 (Java Development Kit)
Based on JavaEE, our Workflow runs currently fine with version 1.5 so if you are using newer version, please download and install this 
version to make Workflow works fine. We will support newer version of Java very soon.

2.2. Download eXo Workflow version from: http://forge.objectweb.org/projects/exoplatform/

2.3. Unzip that package under a path that does not contain any space (in Windows).

2.4. Open a shell session and go to the bin/ directory that has just been extracted.

2.5. Then run the command :
	Windows:
		eXo.bat run
	Linux, Unix, Mac OS
	chmod u+x *.sh ./eXo run

2.6. Open your web browsers, now eXo Workflow can run on FireFox 2 or newer, Internet Explorer 6 or newer 
(we recommend using FireFox 3+ or Internet Explorer 7+ for the best result) 
and navigate to URL: http://localhost:8080/portal

2.7. When the page has been loaded, click "Login" on the top right corner. Specify the username "root" and the password "exo".


3. RELEASE NOTES 
---------------------------------------------------

** In Workflow-1.0.6, we fixed:

** Bug
    * [WF-78] - Fix failing tests

** Improvement
    * [WF-55] - Build, quality and automation improvements

** Task
    * [WF-77] - Correct label in Workflow
    * [WF-79] - center the icon to avoid ambiguity in the Task validation tab
    * [WF-80] - Release WF 1.0.6

** Sub-task
    * [WF-52] - Fix duplicated dependencies
    * [WF-53] - Fix missing dep in exo.ecm.workflow.web.portal
    * [WF-54] - Cleanup and reactivate tests
    * [WF-56] - Upgrade to exo parent 8
    * [WF-59] - Cleanup POMs



** In Workflow-1.0.5, we fixed:

** Task
    * [WF-36] - Release WF 1.0.5
    * [WF-39] - Cleanup the build process to be able to deploy on eXo Nexus with the release plugin for 1.0.x
    * [WF-44] - Publish WF 1.0.5 SNAPSHOT aritfacts in the snapshots repo
    * [WF-48] - Remove unused dependencies in pom.xml
    * [WF-49] - Release JBPM 3.0.1
    * [WF-51] - Upgrade to use jBPM 3.0.2

** Sub-task
    * [WF-40] - Build - Cleanup the profile with properties, remove the reporting and emma config, add parent pom v6
    * [WF-43] - Build - Integrate module.js in the project to be used by exopackage and maven-exobuild-plugin
    * [WF-45] - Extract jbpm 3.0 in a separate trunk and tags/3.0
    * [WF-46] - Use Kernel, Core, JCR, PC, Portal SNAPSHOTs
    * [WF-47] - Fix JBPM 3.0-SNAPSHOT issue (clean by hudson because a 3.0 is out), proably a 3.0.1 is needed.



** In Workflow-1.0.4, we fixed some problems:
	
** Bugs
	-[WF-10] Can not open controller workflow portlet
  -[WF-11] Remove WARNING message when run bonita engine

** Tasks
	-[WF-24] Integrate with JCR 1.10.4
	-[WF-25] Update parent pom to use 1.1.1
	-[WF-26] Consistent Application data folder in organization configuration

** Other resources and links
	Company site        http://www.exoplatform.com
	Community JIRA      http://jira.exoplatform.org
	Comminity site      http://www.exoplatform.org
	Developers wiki     http://wiki.exoplatform.org
	Documentation       http://docs.exoplatform.org 


4. MIGRATION GUIDE
---------------------------------------------------

Workflow can be reached at:

   Web site: http://www.exoplatform.com
						 http://www.exoplatform.vn
   	 E-mail: exoplatform@ow2.org
						 exo-ecm@ow2.org
						

Copyright (C) 2003-2007 eXo Platform SAS.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Affero General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, see<http://www.gnu.org/licenses/>.
